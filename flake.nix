{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    systems.url = "github:nix-systems/default";
  };

  outputs =
    inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      perSystem =
        {
          config,
          self',
          pkgs,
          lib,
          system,
          ...
        }:
        let
          libs = with pkgs; [
            # GL
            libGL
            glfw3-minecraft

            # audio
            libpulseaudio
            openal

            flite

            jdk21
          ];
        in
        {
          devShells.default = pkgs.mkShell {
            nativeBuildInputs = with pkgs; [
              jdk21
            ];

            buildInputs = libs;
            LD_LIBRARY_PATH = lib.makeLibraryPath libs;

            env = {
              JAVA_HOME = "${pkgs.jdk21}/lib/openjdk";
            };
          };
        };
    };
}
