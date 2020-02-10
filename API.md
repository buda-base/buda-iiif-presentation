# BUDA IIIF Presentation API

This document describes the various URIs implemented by the [IIIF Presentation API v2.1.1](https://iiif.io/api/presentation/2.1/) on BUDA. They follow the [URI Pattern recomendations](https://iiif.io/api/presentation/2.1/#a-summary-of-recommended-uri-patterns). Note that in most cases you don't need to determine the URI of the collections and manifests yourself but only need to get them from the BUDA platform by clicking on the IIIF button on a page where images are visible.

The URIs are composed of segments that we will first define:
- `{instanceId}` is the prefixed version of the URI of an instance in BUDA. For instance `bdr:MW22084` is the prefixed version of `http://purl.bdrc.io/resource/MW22084`. Note that this can also be the URI of a part of an instance (which are also instances in the BUDA model), for instance `bdr:MW22084_S0001` is the first section of `bdr:MW22084`
- `{imageGroupId}` is the prefixed version of the URI of an image group, for instance `bdr:I0886`
- `{imageInstanceId}` is the prefixed version of the URI of an image instance. Unless several image instances are available for an instance (which we do not currently have), this is not mandatory. For instance, `bdr:W22084` is the URI of the image instance that we have for the instance `bdr:MW22084`.
- `{imageNumRange}` indicates a subset of image sequence numbers that should be taken from a volume, it is in the format {startImgNum-endImgNum} and is always optional, for example `3-6` means `image number 3, 4, 5 and 6`.

All the URLs given in the rest of the document should be considered to be prefixed with `https://iiifpres.bdrc.io/` (note that `http` should work too).

### Collections

Collection URIs implemented on BUDA are:

- `/collection/wio:{instanceId}::{imageInstanceId}` (`::{imageInstanceId}` is optional) a collection listing the parts of the instance as subcollections and the corresponding image groups as manifests, this is probably what you're looking for ([example](http://iiifpres.bdrc.io/collection/wio:bdr:MW22084))
- `/collection/wi:{instanceId}::{imageInstanceId}` (`::{imageInstanceId}` is optional) a collection listing the image groups corresponding to a work as manifests ([example](http://iiifpres.bdrc.io/collection/wi:bdr:MW22084))
- `/collection/i:{imageInstanceId}` a collection with the image groups of an image instance as manifests ([example](http://iiifpres.bdrc.io/collection/i:bdr:W22084))

### Manifests

The manifest URIs implemented on BUDA are:

- `/v:{imageGroupId}::{imageNumRange}/manifest` for the manifest of an image groups ([example for page 3 and 4 of a volume](http://iiifpres.bdrc.io/v:bdr:I0886::3-4/manifest))
- `/vo:{imageGroupId}::{imageNumRange}/manifest` idem, with the outline ([example](http://iiifpres.bdrc.io/vo:bdr:I0891/manifest))
- `/wv:{instanceId}::{imageGroupId}/manifest` for the manifest of a subset of an image group corresponding to a part of an instance (example: [part of bdr:I0946 corresponding to bdr:MW22084_0193](http://iiifpres.bdrc.io/wv:bdr:MW22084_0193::bdr:I0946/manifest))
- `/wvo:{instanceId}::{imageGroupId}/manifest` idem, with the outline ([example](http://iiifpres.bdrc.io/wvo:bdr:W22084_0193::bdr:I0946/manifest))
