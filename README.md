A synchronization application between SugarCRM and your Android device.

README
======

Preamble
--------
Copyright 2011 Vicent Seguí Pascual

This file is part of Sweet. Sweet is free software: you can
redistribute it and/or modify it under the terms of the GNU General
Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

Sweet is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
for more details. You should have received a copy of the GNU General
Public License along with Sweet.

If not, see http://www.gnu.org/licenses/. 

Disclaimer 
---------- 

Sweet right now is alpha software. It can delete your contacts or mix
them up. As of now, use in production servers and/or phones is discouraged. 
You are *warned*


Description
-----------

Sweet is an Android application that can sync your SugarCRM contacts
with your Android mobile phone. As of now, it will perform a two way
sync the following fields:

- First and last name
- Title and Account Name (company)
- Work phone, Mobile phone and Fax
- Primary email
- Primary postal address


Sweet features editing contacts offline in your phone and uploading
them to the SugarCRM at a later time works. In case of a conflict it
will present you with a UI showing the differences so you can resolve
them and save the changes in the server and the phone. Due to
limitations in stock the Android contact editor a custom interface is
provided to edit contacts. In order to show this interface one must
press the SugarCRM profile entry in the contact detailed view, using
the contact editor will not work. Adding contacts in the phone is also 
supported and will be accordingly uploaded to the server but you have 
to use the Sweet contact editor instead of the stock Android editor. 
Do not set this editor as the default since it can't handle other accounts.  


Known limitations and bugs
--------------------------
- Only one SugarCRM account in your phone.
- Limited and fixed number of contact fields.
- Has bugs


TODO
----
- Bug fixing
- Decide what to do with deleted contacts. Right now nothing is done
- Allow multiple accounts
- Revisit how many and which fields are allowed and how they map to SugarCRM.


FAQ
----

Q: How do I download Sweet?

A: Sweet is available in the Android Market place. 


Q: Can I edit contacts offline?  

A: Yes, you can edit offline contacts in your phone and the changes will be synced.


Q: What version of SugarCRM does Sweet support?

A: Sweet has been tested with SugarCRM 6.1 but should work with any version supporting v2 of the REST API.


Q: What version of Android does Sweet support?

A: Sweet has been tested with 2.1, 2.2 and 2.3. It will not work with earlier version due to changes in the Contacts API.


Q: I have a bug where can I report it?

A: Sweet development is going on at [Github](https://github.com/vseguip/Sweet). You can send bug reports to https://github.com/vseguip/Sweet/issues.


Q: I can't login but I'm sure my username, password and server are correct, what is going on?

A: Maybe your SugarCRM account is backed by an LDAP server in which case you may need to send the password unencrypted. As of version 0.2 Sweet supports sending passwords in the clear but please make sure you use an https connection to avoid eavesdropping. 


Q: I have a patch where can I send it?

A: Right now I'm not accepting patches since the project is done as part of my final career project. 

