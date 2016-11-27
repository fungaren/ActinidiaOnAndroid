# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )

# Specifies a path to native header files.
include_directories(src/main/lua-5.3.3/)
add_library( lua-lib
             SHARED
             src/main/lua-5.3.3/lapi.c
             src/main/lua-5.3.3/lcorolib.c
             src/main/lua-5.3.3/ldump.c
             src/main/lua-5.3.3/llex.c
             src/main/lua-5.3.3/lopcodes.c
             src/main/lua-5.3.3/lstrlib.c
             src/main/lua-5.3.3/lauxlib.c
             src/main/lua-5.3.3/lctype.c
             src/main/lua-5.3.3/lfunc.c
             src/main/lua-5.3.3/lmathlib.c
             src/main/lua-5.3.3/loslib.c
             src/main/lua-5.3.3/ltable.c
             src/main/lua-5.3.3/lundump.c
             src/main/lua-5.3.3/lbaselib.c
             src/main/lua-5.3.3/ldblib.c
             src/main/lua-5.3.3/lgc.c
             src/main/lua-5.3.3/lmem.c
             src/main/lua-5.3.3/lparser.c
             src/main/lua-5.3.3/ltablib.c
             src/main/lua-5.3.3/lutf8lib.c
             src/main/lua-5.3.3/lbitlib.c
             src/main/lua-5.3.3/ldebug.c
             src/main/lua-5.3.3/linit.c
             src/main/lua-5.3.3/loadlib.c
             src/main/lua-5.3.3/lstate.c
             src/main/lua-5.3.3/ltm.c
             src/main/lua-5.3.3/lvm.c
             src/main/lua-5.3.3/lcode.c
             src/main/lua-5.3.3/ldo.c
             src/main/lua-5.3.3/liolib.c
             src/main/lua-5.3.3/lobject.c
             src/main/lua-5.3.3/lstring.c
             src/main/lua-5.3.3/lzio.c )


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       lua-lib
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )