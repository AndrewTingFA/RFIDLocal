RFIDLocal
=========
RFID Localisation Experiment using OpenBeacon

This is part of the code used in the experiments for my thesis on RFID localisation.
Some of the earlier functionality has unfortunately been lost as I've changed some stuff around.



It produces several logfiles at various points.

tagUpdateLogfile.log
The most important is a tagUpdateLogfile.log, which is contains updates on the minimum receivable power level of the RFID tags, both reference and tracking.

Each log is logged on a separate line, such as the one below.

1367739758779: Reader=130.102.86.134Tag=043APower=2

They follow the form 

<Unix Time stamp>: Reader=<IP>Tag=<ID>Power=<Transmit Power>

The Tag ID remains in hexadecimal format, and the transmit power value range from 0 to 3.

tagLocationLogfile.log
Another important file is the tagLocationLogfile.log. This contains a log of the x and y coordinates of the tags.

Each log is logged on a separate line, such as the one below.

1367739758875: Tag=03CF x=100.00000000000001 y=71.42857142857143

They follow the form 

<Unix Time stamp>: Tag=<ID> x=<x-value> y=<y-value>

The Tag ID remains in hexadecimal format, and the x and y values are doubles.

<tag_name>_<k>.log
This is an example of the log file produced by the parser. 
The parse parses the tagUpdateLogfile.log to produce these files, which represents the displacement error. The tag_name is the id of the tag in hexadecimal format, the k is the k value used in the k-NN algorithm.

Each line is a separate reading contains the displacement error, such as the one below.

182.00274723201295
