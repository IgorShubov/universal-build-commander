package com.ubc

enum BuildToolDescriptor {

    MAVEN('Maven', ['**/pom.xml']),
    GRADLE('Gradle', ['**/build.gradle']),
    ANT('Ant', ['**/build.xml']),
    MAKE('Make', ['**/GNUmakefile', '**/makefile', '**/Makefile', '**/*.mk']),
    NANT('NAnt', ['**/*.build.xml']),
    NATIVE('Native', ['**/*.cpp', '**/*.cc', '**/*.cxx', '**/*.h', '**/*.hpp', '**/*.hh', '**/*.hxx', '**/CMakeLists.txt']),
    MSBUILD('MSBuild', ['**/*.proj']),
    VISUALSTUDIO('Visual Studio', ['**/*.sln', '**/*.csproj', '**/*.vbproj', '**/*.vcproj']),
    POWERSHELL('Powershell', ['**/*.ps1', '**/*.PS1'])

    final String id
    final List patterns
    static final Map map

    static {
        map = [:] as TreeMap
        values().each { tool ->
            map.put(tool.id, tool)
        }
    }

    private BuildToolDescriptor(String id, List patterns) {
        this.id = id
        this.patterns = patterns
    }

    static getBuildToolDescriptor(String id) {
        map[id]
    }
}