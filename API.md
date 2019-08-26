# BUDA IIIF Presentation API

This document describes the various URIs implemented by the [IIIF Presentation API v2.1.1](https://iiif.io/api/presentation/2.1/) on BUDA. They follow the [URI Pattern recomendations](https://iiif.io/api/presentation/2.1/#a-summary-of-recommended-uri-patterns). Note that in most cases you don't need to determine the URI of the collections and manifests yourself but only need to get them from the BUDA platform by clicking on the IIIF button on a page where images are visible.

The URIs are composed of segments that we will first define:
- `{workId}` is the prefixed version of the URI of a work in the BUDA database. For instance `bdr:W22084` is the prefixed version of `http://purl.bdrc.io/resource/W22084`. Note that this can also be the URI of a part of a work (which are also works in the BUDA model), for instance `bdr:W22084_S0001` is the URI of a part of `bdr:W22084`
- `{volumeId}` is the prefixed version of the URI of a volume, for instance `bdr:V22084_I0886`
- `{itemId}` is the prefixed version of the URI of an item. Unless several items are available for a work (which doesn't currently happen), this is never mandatory
- `{imageNumRange}` indicates a subset of image sequence numbers that should be taken from a volume, it is in the format {startImgNum-endImgNum} and is always optional
- `{scheme}` can be `http` or `https`

### Collections

Collection URIs implemented on BUDA are:

- `{scheme}://iiifpres.bdrc.io/2.1.1/collection/wio:{workId}::{itemId}` (`::{itemId}` is optional) a collection listing the parts of the work as subcollections and the corresponding volumes as manifests, this is probably what you're looking for ([example](http://iiifpres.bdrc.io/2.1.1/collection/wio:bdr:W22084))
- `{scheme}://iiifpres.bdrc.io/2.1.1/collection/wi:{workId}::{itemId}` (`::{itemId}` is optional) a collection listing the volumes corresponding to a work as manifests ([example](http://iiifpres.bdrc.io/2.1.1/collection/wi:bdr:W22084))
- `{scheme}://iiifpres.bdrc.io/2.1.1/collection/i:{itemId}` a collection with the volumes of an item as manifests ([example](http://iiifpres.bdrc.io/2.1.1/collection/i:bdr:I22084))

### Manifests

The manifest URIs implemented on BUDA are:

- `{scheme}://iiifpres.bdrc.io/2.1.1/v:{volumeid}::{imageNumRange}/manifest` for the manifest of a volume ([example for page 3 and 4 of a volume](http://iiifpres.bdrc.io/2.1.1/v:bdr:V22084_I0886::3-4/manifest))
- `{scheme}://iiifpres.bdrc.io/2.1.1/vo:{volumeid}::{imageNumRange}/manifest` for the manifest of a volume, with the outline encoded as ranges
- `{scheme}://iiifpres.bdrc.io/2.1.1/wv:{workid}::{volumeId}/manifest` for the range of a volume corresponding to a work (usually a part of the work) ([example](http://iiifpres.bdrc.io/2.1.1/wv:bdr:W22084_0193::bdr:V22084_I0946/manifest))
- `{scheme}://iiifpres.bdrc.io/2.1.1/wvo:{workid}::{volumeId}/manifest` for the range of a volume corresponding to a part of a work, with the outline encoded as ranges ([example](http://iiifpres.bdrc.io/2.1.1/wvo:bdr:W22084_0193::bdr:V22084_I0946/manifest))
