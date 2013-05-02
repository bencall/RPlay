RPlay v0.02
==============
Benjamin de Callatay <bencall@hotmail.com>
April 22, 2011

What it is
----------
This program is in alpha version.
It emulates an Airport Express for the purpose of streaming music from iTunes and compatible iPods. It implements a server for the Apple RAOP protocol.

Installation
------------
Double clicking on RPlay.jar in the DIST folder should be enough...

Thanks
------
Big thanks to David Hammerton for releasing an ALAC decoder and to soiaf for porting it to Java (https://github.com/soiaf/Java-Apple-Lossless-decoder).
Thanks to Jame Laird for his C implementation (shairport - https://github.com/albertz/shairport)
Thanks to anyone involved in one of the libraries i used for creating this software.

Libraries & References
----------------------
These libraries are included in RPlay:
* http://www.bouncycastle.org/latest_releases.html
* http://commons.apache.org/
* https://github.com/albertz/shairport
* https://github.com/soiaf/Java-Apple-Lossless-decoder

Contributors
------------
* [David Hammerton]
* [James Laird]
* [soiaf]
* [adeward] (https://github.com/adeward)
* [jblezoray] (https://github.com/jblezoray)
* [Maik Schulz] for the Mac OS X bundle
* Everyone who has helped with shairport, the alac decoder (or the java port of it), apache commons lib or bouncycastle lib (see their README)

Changelog
---------
* 0.01a  April 22, 2011
    * initial release: able to communicate with iTunes (RTSP server ok)
* 0.01b  May 01, 2011
    * First working version. Still buggy. Usable.   
* 0.01c   May 06, 2011
    * First binary version. Need feedback.
* 0.02    April 29, 2013
    * Fixed auto-startup and enabled RPlayNoGui.jar.