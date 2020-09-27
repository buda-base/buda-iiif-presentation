# IIIF Presentation for the BUDA Platform 

This repository contains a servlet generating manifests and collections for BDRC.

## API

See [API.md](API.md) for the API.

## Running

- `mvn test` runs the tests

To serve locally:

```

mvn package
java -Dserver.port=8080 -Dspring.profiles.active=local -Diiifpres.configpath=src/main/resources -jar target/buda-iiif-presentation-exec.war
```

Ex: 
- http://localhost:8080/v:bdr:I0886/manifest
- http://localhost:8080/collection/i:bdr:W22084
- http://localhost:8080/v:bdr:IEAP676-7-12/manifest

This uses S3 to fetch a `dimension.json` file, using the default credential provider, make sure the correct environment vars / properties are set.

## Copyright and License

All the code and API are `Copyright (C) 2017-2020 Buddhist Digital Resource Center` and are under the [Apache 2.0 Public License](LICENSE).
