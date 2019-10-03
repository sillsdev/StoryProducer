with import <nixpkgs> {};

(buildFHSUserEnv {
  name = "android-studio-less-gradle-builder";
  targetPkgs = pkgs: (with pkgs; [
    stdenv.cc.cc.lib
    pkgsi686Linux.stdenv.cc.cc.lib
    zlib
    pkgsi686Linux.zlib
    jdk
    which
    ]);

    runScript = ''
      bash -c "JAVA_HOME=${jdk.home} bash"
      '';
}).env
