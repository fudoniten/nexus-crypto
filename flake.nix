{
  description = "Nexus Crypto Library and Keygen";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-22.11";
    utils.url = "github:numtide/flake-utils";
    helpers = {
      url = "git+https://git.fudo.org/fudo-public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, utils, helpers, ... }:
    utils.lib.eachDefaultSystem (system:
      let pkgs = import nixpkgs { inherit system; };
      in {
        packages = rec {
          default = nexus-keygen;
          nexus-keygen = helpers.packages."${system}".mkClojureBin {
            name = "org.fudo/nexus-keygen";
            primaryNamespace = "nexus.keygen";
            src = ./.;
          };
        };

        devShells = rec {
          default = update-deps;
          update-deps = pkgs.mkShell {
            buildInputs = with helpers.packages."${system}";
              [ updateClojureDeps ];
          };
        };
      });
}
