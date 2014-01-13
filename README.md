# MIRROR Spaces Service
The MIRROR Spaces Service is the core component of the [MIRROR Spaces Framework (MSF)][1]. It is realized as XMPP component and implemented as plugin for the [Openfire XMPP server][2].

## Build
An documentation how to setup an development environment for Openfire is described [here][3]. A developer guide for Openfire plugins including information how to build it is available [here][4].

## Install
To install the plugin perform the following steps:

1. Open the administration console of Openfire and click on the "Plugins" tab.
2. In the "Upload Plugin" section of the page, select the spacesService.jar file and submit it by pressing on the "Upload Plugin" button.
3. After a few seconds, new entry "MIRROR Spaces Service" should show up in the plugins list. You can validate the installation by opening the info log ("Server" > "Server Manager" > "Logs" > "Info") , which should contain a line "MIRROR Spaces plugin initialized.".
4. Open the new "MIRROR Spaces" tab and click on "Settings" to open the general settings for the service.
5. Check the entries in the "Server Configuration" box. Update the settings if necessary.

## Update
This is the first version of the plugin for Openfire 3.8.x. If you want to upgrade an older version of Openfire, perform the following steps:

1. Open the "Plugins" tab of the openfire administration console and delete the old "MIRROR Spaces Service". Note: Spaces and stored data will remain unaffected.
2. Upgrade your openfire installation as described in the upgrade guide.
3. Deploy the plugin as described above.

## Configuration
If you also deployed the MIRROR Persistence Service you need to connect the services in order to work properly:

1. Open the new "MIRROR Spaces" tab and click on "Settings" to open the general settings for the service.
2. Select the checkbox "Connect to the MIRROR Persistence Service." in the persistence settings.
3. Submit the change by pressing the "Save" button

## Usage
API specifications and examples can be found in the `manual.pdf` packaged with the plugin.

## License
The MIRROR Spaces Service is released under the [Apache License 2.0][5].

## Changelog

v0.6.1 -- January 9, 2014

* [FIX] Bugfix release.

v0.6 -- October 2, 2013

* [NEW] Runs with Openfire 3.8.2. This version is NOT compatible with previous versions of Openfire.
* [NEW] Added server configuration check to administration console.
* [NEW] Repairs broken pubsub subscriptions.
* [UPDATE] Changed plugin identifier.

v0.5 -- April 15, 2013

* [FIX] Pubsub node subscriptions are now validated an repaired when the service is initialized.
* [UPDATE] Changed name displayed in administration console and service discovery.
* [UPDATE] Changed description displayed in administration console.
* [NEW] Added support for MIRROR Persistence Service.
* [NEW] Data objects with more than 64k characters are now rejected.
* [NEW] Added support for Interop Data Models.
* [NEW] Added interface to request version information.

v0.4.3 -- October 10, 2012

* Expected schema location will now also be to data object published on private/team spaces, too.
* Fixed bug breaking CDM attribute update on private spaces.

v0.4.2 -- October 10, 2012

*  bug breaking the model validation.

v0.4.1 -- September 4, 2012

*Fixed bug preventing data objects to be published on orga spaces.

v0.4.0 -- Milestone 5 -- August 24, 2012

* Tagged milestone release.

v0.4.0 RC2 -- August 17, 2012

* Added support for organizational spaces.
* Release candidate.

v0.3.0 -- July 16, 2012

* Added support for CDM version 1.0.
* Base implementation for packet filtering added.

v0.2.2 -- July 12, 2012

* Fix: Set pubsub#max_items=0 when space is configured to be non-persistent.
* Improved space consistency validation.

v0.2.1 -- April 26, 2012

* Fix: Set pubsub#max_items=MAX_INT when a space is reconfigured to be persistent.

v0.2.0 -- Milestone 2 -- March 16, 2012

* Fix: Nickname handling in MUC rooms.
* Minor updates.

v0.1.5 -- March 11, 2012

* Added integrity check for spaces. The check is performed during startup.

v0.1.4.1 -- March 5, 2012

* Fix: PubSub nodes will now be available after server restart.
* Fix: MUC rooms will now be available after server restart.
* Fix: MUC rooms persistence settings corrected.

v0.1.3 -- February 29, 2012

* Added support for the configuration of team spaces.

v0.1.2 -- February 27, 2012

* Added support for the creation of team spaces.
* Added support for the discovery of team spaces.

v0.1.0.1 -- February 17, 2012

* Fixed items discovery.

v0.1.0 -- Milestone 1 -- February 17, 2012

* First milestone release.
* Fixed configuration field mapping.
* Fixed persistence handling.
* Fixed authorization issues with node discovery.
* Several bug fixes.

v0.0.4 -- February 13, 2012

* Added configuration validation.
* Changed membership from user node-id to bare JID.
* Several bug fixes.

v0.0.3 -- February 6, 2012

* Added name configuration of private spaces.
* Added deletion of spaces.

v0.0.2 -- February 2, 2012

* Add creation of private spaces.
* Added space discovery as specified in XEP-0030.

v0.0.1 -- January 31, 2012

* Requires Openfire 3.7.0.
* Initial release.

  [1]: https://github.com/MirrorIP
  [2]: http://www.igniterealtime.org/projects/openfire/
  [3]: http://community.igniterealtime.org/docs/DOC-1020
  [4]: http://www.igniterealtime.org/builds/openfire/docs/latest/documentation/plugin-dev-guide.html
  [5]: http://www.apache.org/licenses/LICENSE-2.0.html