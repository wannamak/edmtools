# [edmtools](https://github.com/wannamak/edmtools)

edmtools are a set of analysis tools built on the unattributed jpihack.cc
for the EDM-series of engine data monitors produced for general aviation
aircraft by J.P. Instruments, Inc.

## Installation

`$ git clone https://github.com/wannamak/edmtools.git`

## Prerequesites

  * Java > 1.5
  * Ant
  * The Google protocol buffer compiler >= 3.0.0.  As of this writing it is not yet released;
    clone [the repository](https://github.com/google/protobuf) and build it.

## Running

  * `$ ant`
  * `$ scripts/JpiDecode --list file.JPI`
  * `$ scripts/JpiDecode --flight xxx file.JPI`
  * `$ scripts/JpiDecode --v 3 --flight xxx file.JPI`
  * `$ scripts/JpiDecode --flight xxx file.JPI`

## scripts/JpiDecode

Prints a JPI binary file in a human-readable (protocol buffer) format to STDOUT.

<dl>
<dt>-v</dt>
<dd>verbosity, 0-3</dd>
<dt>-list</dt>
<dd>list the flight numbers in the JPI file to standard out</dd>
<dt>-flight</dt>
<dd>select a particular flight number to convert</dd>
</dl>
  
## scripts/JpiRewrite

Extracts some or all of a JPI file to another JPI file.
This was written to extract samples for functional tests.

<dl>
<dt>-v</dt>
<dd>verbosity, 0-3</dd>
<dt>-flights</dt>
<dd>comma-separated list of flight numbers</dd>
<dt>-reg</dt>
<dd>replace the registration (eg tail or serial number) with an arbitrary string</dd>
</dl>

## API

```
import edmtools.JpiDecoder;
import edmtools.JpiDecoder.JpiDecoderConfiguration;
import edmtools.JpiInputStream;
import edmtools.Proto.JpiFile;

JpiFile jpiFile = JpiDecoder.decode(
    new JpiInputStream("DOWNLOADED.JPI"),
    JpiDecoderConfiguration.newBuilder().build());
```

## JPI File Format notes

The JPI binary file format stores a series of value deltas.  Data corruption
therefore insidiously affects a metric for the remainder of the flight.  To guard against this,
JPI prefixes each data record with a repeated marker and also checksums each data record with 
a postfix byte.  JPI tools will abort processing upon encountering checksum errors; this toolkit
will continue processing but log the error in the parse_warning field.  The user should treat
any remaining output following a parse_warning with suspicion.  Data corruption in the masks 
at the beginning of each record will render the record unparseable as the expected vs actual
quantity of deltas will differ and the program will under- or over-read the record. 

## Links
  * [JP Instrument downloads](https://www.jpinstruments.com/technical-support/software-downloads/)

