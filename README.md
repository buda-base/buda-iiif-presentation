# IIIF Presentation for the BUDA Platform

This repository contains a servlet generating manifests and collections for BDRC.

## Running

- `mvn test` runs the tests
- `mvn jetty:run` serves the app locally
- `mvn war:war` produces a war file

Ex: http://localhost:8080/2.1.1/v:bdr:V22084_I0886/manifest

This uses S3 to fetch a dimension.json file, using the default credential provider, make sure the correct environment vars / properties are set.

## Copyright and License

All the code and API are `Copyright (C) 2017 Buddhist Digital Resource Center` and are under the [Apache 2.0 Public License](LICENSE).
