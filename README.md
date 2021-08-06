# DMX CSV Importer

A simple CSV importer to import and update topic entities and their simple 1st-level childs into [DMX 5.0-beta-7](https://github.com/jri/deepamehta).

Limitations:

 * delimiter: ```|``` (default)
 * data type: Text

## Usage

You need to create yourself a `File` topic first.

This can either be done through issueing a `GET` request to the FileService endpoint, e.g.: `http://localhost:8080/files/file/notes.csv` or through uploading a `.csv` file interactively using the [dmx-upload-dialog](https://github.com/mukil/dmx-upload-dialog) plugin.

-  Search and reveal the `notes.csv` file on a topicmap.
-  Associate the file topic with the _Topic Type_ you want to import topics of
--  Note: the columns in your `.csv` file to import must match the child topic `typeURI`s.
-  Retype the association to a `File Import` edge
-  Fire the custom `Import CSV` context command on the `notes.csv` file topic

See [here](https://github.com/mukil/dmx-csv/tree/master/src/test/resources) to find an exemplary `.csv` file for importing `Note` and `Bookmark` topics.

To run the import process use the *Import CSV* command available on all `File`-topics which end on `.csv`.

## Download

At some point in the future you may find the latest versions of this plugin at [https://download.dmx.systems/plugins/](https://download.dmx.systems/plugins/).

## Requirements

 * [DMX](https://github.com/jri/deepamehta) 5.0-beta-7
 * Write access to `dmx.filerepo.path` (see DM4 `config.properties` file)


### Import some topics from a CSV file

To create some *Note* topics use a CSV file (a simple text-file) structured like the following:

```
csv.example | dmx.notes.title | dmx.notes.text
one         | check this      | with content
two         | check this too  | and with another content
```

You could then upload this file through using the *Import CSV* command of the *Note* topic type. For each line, beginning at the second, there will be a *Note* created in your DeepaMehta 4 installation.

Alternatively: A CSV file to import some *Web Resource* topics would need its contents structured like the following:

```
deep.web | dmx.base.url                        | dmx.bookmarks.bookmark_description
site     | https://dmx.systems                 | <h1>DMX Platform</h1><p class="slogan">Cope With Complexity</p>
plugins  | http://download.dmx.systems/plugins | <h1>DMX Plugins</h1><p class="slogan">Download Extensions</p>
demo     | https://demo.dmx.systems            | <h1>Demo Server</h1><p>try it now</p>
ci       | http://download.dmx.systems/ci      | <h1>Continuous Integration</h1><p>fresh nightly builds</p>
```


### Update existing topics

As of DMX 5.0 updates are done via the core's value integration mechanisms and not (yet) by URI.

This also means, on subsequent imports of your CSV file, there is no deletion mechanism. Once imported topic will not get deleted. Former topics imported will (most probably) become only orphaned topics. 

We opened up an issue to get back the support for deletion _and_ continous updates by URI in the dmx-platform repository:
- See: Value integration treats an URI as an identity attribute

### CSV Format Description

 * topic URI prefix as the first entry (row: 0, col: 0)
 * all direct child topics are the remaining header columns
 * one topic per row with topic URI suffix in the first column

## Licensing

DMX CSV is available freely under the GNU Affero General Public License, version 3.

All third party components incorporated into the DMX CSV Software are licensed under the original license provided by the owner of the applicable component.

## Release History

**1.1.3**, Upcoming

- Compatible with DMX 5.2 API
- Added configuration option: `<dmx.csv.delete_instances_on_update>false</dmx.csv.delete_instances_on_update>` (pom.xml style)
- Removed transactions per row
- Support for referencing entity topics from import data by URI (use the prefix `#ref_uri:` and then follow up with the entity URI)
- Note: Many separator to represent multiple values in one column has change to ``;``!

**1.1.2**, Jan 02, 2020

- Compatible with DMX 5.1 API

**1.1.1**, Aug 15, 2020

- Adapted to latest changes in DMX 5.0 API
- Fixes https://github.com/mukil/dmx-csv/issues/7
- Fixes incorrect status count on subsequent imports (updates)

**1.1.0**, Jun 24, 2020

- Compatible with upcoming DMX 5.0 API

**1.0.0**, Jun 19, 2020

- Adapted to be compatible with DMX 5.0-beta-7
- Allowing for imports of many values from a single column (Seperator: ``,``)

Former releases were undocumented.

### Authors

(C) Malte Reißig 2013-2020<br/>
(C) Danny Graf 2013-2015
