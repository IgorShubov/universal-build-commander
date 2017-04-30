package com.ubc

import java.util.regex.Pattern

class BuildToolFinder {

    /**
     * The build tools currently supported by the tekton transformer
     */
    def static supportedTools = ['Maven', 'Visual Studio', 'Gradle', 'Native', 'Make']

    /**
     * Sorts 2 file strings based on their depth
     *
     * @param f1    the first file path string
     * @param f2    the second file path string
     * @return      the comparison result
     */
    static fileSorter(String f1, String f2) {
        def c1 = (f1 =~ /${Pattern.quote(File.separator)}/).count
        def c2 = (f2 =~ /${Pattern.quote(File.separator)}/).count
        c1 <=> c2 ?: f1 <=> f2
    }

    /**
     * Sorts 2 file objects based on their depth
     *
     * @param f1    the first file path
     * @param f2    the second file path
     * @return      the comparison result
     */
    static fileSorter(File f1, File f2) {
        fileSorter(f1.absolutePath, f2.absolutePath)
    }

    def Map findBuildTool(String path, BuildToolDescriptor preferredBuildTool) {
        def selectedBuildTool, selectedBuildToolDescriptors

        Map<String, List<String>> buildTools = detectBuildTool(path)

        if (buildTools.isEmpty()) {
            throw new IllegalArgumentException("Could not detect a build tool")
        }
        if (buildTools.size() > 1) {
            def supportedMatches = preferredBuildTool ?
                      supportedTools.findAll { buildTools.containsKey(it) && it == preferredBuildTool.id }
                    : supportedTools.findAll { buildTools.containsKey(it) }
            if (supportedMatches.size() == 1) {
                selectedBuildTool = supportedMatches.first()
            }
            else if (supportedMatches.size() > 1) {
                supportedMatches.sort { a, b ->
                    (fileSorter(buildTools.get(a).first(), buildTools.get(b).first()))
                }
                selectedBuildTool = supportedMatches.first()
            }
        }
        if (!selectedBuildTool) {
            selectedBuildTool = buildTools.sort { a, b ->
                fileSorter(a.value.first(), b.value.first())
            }.find()?.key
        }

        selectedBuildToolDescriptors = buildTools.get(selectedBuildTool)

        [
            "buildTool" : selectedBuildTool,
            "buildFiles" : selectedBuildToolDescriptors
        ]
    }


    private Map detectBuildTool(String path) {
        Map buildToolMatches = [:]

        BuildToolDescriptor.values().each {
            String pattern
            if (it.patterns.size() > 1)
                pattern = it.patterns.flatten().join(',')
            else
                pattern = it.patterns.get(0)

            // Find the descriptors, but make sure src/test/* folder is excluded.
            def descriptors = new FileNameFinder().getFileNames(path.replaceAll("%20", " "), pattern, "**/src/test/**/*")
            if (descriptors && descriptors.size() > 0) {
                buildToolMatches.put(it.id, descriptors.sort { a, b ->
                    fileSorter(a, b)
                })
//                System.out.println("Match found for ${it.id}: ${descriptors.join(', ')}")
            }
        }

        buildToolMatches
    }
}
