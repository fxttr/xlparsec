{
  description = "xlparsec";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }: 
      flake-utils.lib.eachDefaultSystem
        (system:
          let pkgs = nixpkgs.legacyPackages.${system}; in
          {
            inherit pkgs;
            devShells.default = pkgs.mkShell {
                buildInputs = [
                  pkgs.sbt
                  pkgs.metals
                  pkgs.openjdk22
                  pkgs.coursier
                  pkgs.spark3
                ];
              };
          }
        );
}