{
  description = "Nexus Crypto Library and Keygen";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-23.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url = "git+https://fudo.dev/public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, ... }:
    utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        inherit (helpers.packages."${system}") mkClojureBin mkClojureLib;
      in {
        packages = rec {
          default = nexus-keygen;
          nexus-keygen-bin = mkClojureBin {
            name = "org.fudo/nexus-keygen-bin";
            primaryNamespace = "nexus.keygen";
            src = ./.;
          };
          nexus-keygen = mkClojureLib {
            name = "org.fudo/nexus-keygen";
            src = ./.;
          };
        };

        devShells = rec {
          default = updateDeps;
          updateDeps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ (updateClojureDeps { }) ];
          };
        };
      });
}
