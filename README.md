# IIIF Presentation for the BUDA Platform

This repository contains a servlet generating manifests and collections for BDRC.

## Running

- `mvn test` runs the tests
- `mvn jetty:run` serves the app locally
- `mvn war:war` produces a war file

Ex: http://localhost:8080/2.1.1/ivn:bdr:I22084_I001::0886/manifest

## Copyright and License

All the code and API are `Copyright (C) 2017 Buddhist Digital Resource Center` and are under the [Apache 2.0 Public License](LICENSE).
