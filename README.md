# Tappy External NFC

This project includes a simple utility for connecting to TappyBLE and TappyUSB devices, initiating tag polling,
and broadcasting the results to other applications that may be listening. Additionally, it has the
capability to automatically launch URLs found on NDEF-formatted tags.

## Project Modules

### app

Play store: [com.taptrack.roaring](https://play.google.com/store/apps/details?id=com.taptrack.roaring)

This is the primary component of the Tappy External NFC project. This app allows the user to select a
combination of TappyUSB and/or TappyBLE devices that the application will then connect to, initiate polling
on, and broadcast responses from using the Intents detailed below.  It can also optionally has
launch URLs read from NDEF-formatted tags similar to Android's built-in behaviour if the user so chooses.
Additionally, if a TappyBLE connection unexpectedly drops, the app will automatically attempt to reconnect.

### echimamish

Play store: [com.taptrack.echimamish](https://play.google.com/store/apps/details?id=com.taptrack.echimamish)

This is another app that serves as an example of listening to the broadcasts sent by the main
External NFC app. It listens for the NDEF_FOUND and TAG_FOUND broadcasts sent by the main app
and displays the data received in a simple view similar to Android's built-in tag viewer.

## Integrating with the Tappy External NFC app

In order to use the broadcasts from the Tappy External NFC app in your own application, you will
have to register a broadcast receiver listening for one of two Intents:

### NDEF Found

This intent is broadcast when a tag is found that contains NDEF-formatted data, it's action string
and extras are as follows:

__Action:__ "com.taptrack.roaring.NDEF_FOUND"

#### Extras
_NfcAdapter.EXTRA_ID_: byte array containing the tag's serial number. For most tags this will either
be 4 or 7 bytes long.

_NfcAdapter.EXTRA_NDEF_MESSAGES_: Parcelable array containing the NdefMessage found on the tag.

_"com.taptrack.roaring.extra.TAG_TYPE"_: Integer describing the type of tag that was tapped. The meaning
of these integers can be found either in the [TagTypes](https://github.com/TapTrack/TappyBLE/blob/master/tappy-constants/src/main/java/com/taptrack/tcmptappy/tappy/constants/TagTypes.java)
file in the tappy-constants module of the full TappyBLE SDK or in the Tappy's documentation.


### Tag Found

This intent is broadcast when a tag is found that does not contain NDEF-formatted data, it's action string
and extras are as follows:

__Action:__ "com.taptrack.roaring.TAG_FOUND"

#### Extras
_NfcAdapter.EXTRA_ID_: byte array containing the tag's serial number. For most tags this will either
be 4 or 7 bytes long.

_"com.taptrack.roaring.extra.TAG_TYPE"_: Integer describing the type of tag that was tapped. The meaning
of these integers can be found either in the [TagTypes](https://github.com/TapTrack/TappyBLE/blob/master/tappy-constants/src/main/java/com/taptrack/tcmptappy/tappy/constants/TagTypes.java)
file in the tappy-constants module of the full TappyBLE SDK or in the Tappy's documentation.
