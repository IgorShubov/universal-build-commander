package com.ubc.plugin.maven

import groovy.xml.dom.DOMCategory
import org.apache.commons.lang.StringUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.xerces.parsers.DOMParser
import org.apache.xml.serialize.OutputFormat
import org.apache.xml.serialize.XMLSerializer
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.Element

class SetupMavenTask extends DefaultTask {

    /**
     * The Gradle task that sets up Maven
     */
    @TaskAction
    def SetupAction() {
        println "\n>>> Extracting Maven v$project.mavenPlugin.mvnVersion from zip..."
        extractMaven()

        println "\n>>> Setting up Maven settings.xml...\n"
        configureMavenSettings()

        if (project.mavenPlugin.toolchains.size() > 0) {
            println ">>> Setting up Maven toolchains..."
            configureMavenToolchains()
        }
    }

    /**
     * Configure the Maven Toolchains plugin support
     */
    def configureMavenToolchains() {
        def toolchains = project.mavenPlugin.toolchains
        File toolchainFile = new File(System.getProperty("user.home"), ".m2/toolchains.xml")

        // does toolchains file exist?
        if (toolchainFile.exists()) {
            def toolchainXml = new XmlSlurper().parse(toolchainFile)
            toolchains.each { key, value ->
                // does it contain required tool/version?
                def foundToolchain = toolchainXml.toolchain.find {
                    it.type.text() == key && it.provides.version.text() == value
                }
                if (foundToolchain.size() > 0) {
                    // required toolchain is in file, if it's protobuf check it actually exists
                    if (foundToolchain.type.text() == "protobuf") {
                        def protobuf = foundToolchain.configuration.protocExecutable.text()
                        if (protobuf && !new File(protobuf).exists()) {
                            println "\n>>> [WARNING] Required protobuf declaration found in toolchain.xml but couldn't find the executable at the expected location: $protobuf\n"
                        } else {
                            println "\n>>> Required protobuf declaration found in toolchain.xml and defined executable exists at $protobuf\n"
                        }
                    }
                } else {
                    println "\n>>> [WARNING] Couldn't find the required toolchain ($key v$value) in $toolchainFile.path\n"
                    // toolchain file exists but doesnt contain required toolchain
                    // TODO download toolchain and add to file
                }
            }
            // no toolchains file
        } else {
            // download toolchain and create toolchain file, currently only supports protobuf
            if (toolchains.containsKey('protobuf')) {
                println "\n>>> No toolchain.xml file found at $toolchainFile.path, installing protobuf and creating one"
                def writer = new StringWriter()
                def toolchainXml = new groovy.xml.MarkupBuilder(writer)

                toolchains.each { key, protoVersion ->
                    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                        project.mavenPlugin.protobufExec = "${project.mavenPlugin.protobufExec + protoVersion}.exe"
                    } else {
                        project.mavenPlugin.protobufExec = "${project.mavenPlugin.protobufExec + protoVersion}-CSL3"
                    }
                    project.mavenPlugin.protobufUrl = project.mavenPlugin.protobufUrl + protoVersion + '/'

                    println "\n>>> Downloading $project.mavenPlugin.protobufExec..."
                    project.ant.get(src: project.mavenPlugin.protobufUrl + project.mavenPlugin.protobufExec, dest: "clean-room-maven")
                    if (Os.isFamily(Os.FAMILY_UNIX))
                        project.ant.chmod(file: "clean-room-maven/$project.mavenPlugin.protobufExec", perm: "ug+rx")

                    toolchainXml.toolchains {
                        toolchain {
                            type("protobuf")
                            provides {
                                version(protoVersion)
                            }
                            configuration {
                                protocExecutable(project.file("clean-room-maven$File.separator$project.mavenPlugin.protobufExec").absolutePath)
                            }
                        }
                    }
                }

                def createdToolchainFile = new File("clean-room-maven/toolchains.xml")
                createdToolchainFile.write(writer.toString())
                println "\n>>> Creating toolchains.xml file..."
                project.mavenPlugin.mvnArgs.add('\"-t' + createdToolchainFile.absolutePath + '\"')
            } else {
                // currently only supporting protobuf so nothing to do here
            }
        }
    }

    /**
     * Configure the Maven settings.xml
     */
    def configureMavenSettings() {
        project.configure(project) {
            new File(project.mavenPlugin.mvnSettings.path).withWriter { writer ->
                writer << this.class.classLoader.getResourceAsStream("build-settings.xml")
            }

            ant.replace(file: project.mavenPlugin.mvnSettings.path) {
                replacefilter(token: "@username@", value: project.mavenPlugin.artifactory.artifactoryUsername)
                replacefilter(token: "@password@", value: project.mavenPlugin.artifactory.artifactoryPassword)
            }

            if (project.properties.containsKey('stageEnv')) {
                project.ant.replace(file: project.mavenPlugin.mvnSettings.path) {
                    replacefilter(token: "@isStage@", value: "-stage")
                }
            } else {
                project.ant.replace(file: project.mavenPlugin.mvnSettings.path) {
                    replacefilter(token: "@isStage@", value: "")
                }
            }

            if (project.mavenPlugin.runSonar) {
                configureSonar()
            }
        }
    }

    void configureSonar() {
        def mvnSettings = project.mavenPlugin.mvnSettings

        DOMParser parser = new DOMParser()
        parser.parse(mvnSettings.toURI().toString())
        Document document = parser.getDocument()
        def root = document.getDocumentElement()
        configureSonarProfile(document, root, project)

        XMLSerializer serializer = new XMLSerializer(new FileOutputStream(mvnSettings.absolutePath), new OutputFormat(document))
        serializer.serialize(document)
    }

    void configureSonarProfile(Document document, Element root, Project project) {

        def indentedAppendChild = { Element parent, Element element ->
            def indent = "    ", newElement, elem = parent
            while (elem && elem != root) {
                indent += "    ";
                elem = elem.parentNode;
            }

            if (parent.hasChildNodes() && parent.lastChild.nodeType == 3 && (parent.lastChild.textContent =~ /^\s*[\r\n]\s*$/).count > 0) {
                parent.insertBefore(document.createTextNode("\n" + indent), parent.lastChild)
                newElement = parent.insertBefore(element, parent.lastChild)
            } else {
                parent.appendChild(document.createTextNode("\n" + indent))
                newElement = parent.appendChild(element)
                parent.appendChild(document.createTextNode("\n" + (indent - "    ")))
            }
            return newElement
        }

        def createEmptyElement = { Element existingElement, String newElement ->
            def element = document.createElement(newElement)
            return indentedAppendChild(existingElement, element)
        }

        def createElement = { Element existingElement, String newElement, String newElementVal ->
            def element = document.createElement(newElement)
            element.appendChild(document.createTextNode(newElementVal))
            return indentedAppendChild(existingElement, element)
        }

        def addSonarProfile = { profiles ->
            def profile = createEmptyElement(profiles, 'profile')
            createElement(profile, 'id', 'sonar')
            def properties = createEmptyElement(profile, 'properties')
            createElement(properties, 'sonar.host.url', "http://$project.mavenPlugin.sonar.sonarServer/sonar")
            createElement(properties, 'sonar.login', project.mavenPlugin.sonar.sonarUsername)
            createElement(properties, 'sonar.password', project.mavenPlugin.sonar.sonarPassword)
            createElement(properties, 'sonar.scm.disabled', 'true')
            def sonarProjectKey = project.mavenPlugin.sonar.sonarProjectKey

            createElement(properties, 'sonar.projectKey', sonarProjectKey)
            createElement(properties, 'sonar.moduleKey', sonarProjectKey + ':${project.artifactId}')
            createElement(properties, 'sonar.projectName', sonarProjectKey + ': ${project.artifactId}')
            createElement(properties, 'sonar.clover.reportPath', "$project.mavenPlugin.buildDir/target/site/clover/clover.xml")
            createElement(properties, 'sonar.clover.codeCoveragePlugin', 'clover')
            if (!StringUtils.isEmpty(project.mavenPlugin.sonar.sonarAnalysisSources)) {
                createElement(properties, 'sonar.sources', project.mavenPlugin.sonar.sonarAnalysisSources)
            }
            if(!StringUtils.isEmpty(project.mavenPlugin.sonar.sonarAnalysisTests)) {
                createElement(properties, 'sonar.tests', project.mavenPlugin.sonar.sonarAnalysisTests)
            }
            if(!StringUtils.isEmpty(project.mavenPlugin.sonar.sonarExclusionSources)) {
                createElement(properties, 'sonar.exclusions', project.mavenPlugin.sonar.sonarExclusionSources)
            }
            if(!StringUtils.isEmpty(project.mavenPlugin.sonar.sonarExclusionTests)){
                createElement(properties, 'sonar.test.exclusions', project.mavenPlugin.sonar.sonarExclusionTests)
            }
        }

        use(DOMCategory) {
            org.w3c.dom.NodeList profilesNL = root.getElementsByTagName("profiles")
            def profilesNode = profilesNL.iterator().next() as Element
            addSonarProfile(profilesNode)
        }
    }

    /**
     * Extract the Maven zip
     */
    def extractMaven() {
        project.copy {
            from { project.zipTree { project.file("clean-room-maven/maven-${project.mavenPlugin.mvnVersion}.zip") } }
            into { "clean-room-maven" }
        }
    }
}