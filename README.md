#FileChief

**FileChief:** a open sourse file load/delivery utility.

###Features

+ send/receive files across the Internet(FTP,SFTP) and locally
+ simple configuration
+ send files to multiple destinatons at once
+ detailed history
+ detect lags and queues
+ email notifications
+ automatic compression(zip,gzip) and rename

###Build

To build FileChief on your machine, checkout the repository, cd into it, and call:
```
mvn clean install
```

###Documentation
FileChief [manuals](./docs)

###Usage
1. build
2. unpack target/filechief*bin.zip into work directory
3. set path to JVM in param.sh(param.bat)
4. if need email notification - set MDC... parameters in param.sh(param.bat),
and add MAIL appender in log4j.properties (log4j.rootCategory=INFO,A2,MAIL)
5. create fileChief.xml
6. check configuration ```./run.sh test``` or ```test.bat```
7. start FileChief ```./run.sh``` or ```start.bat```

###Example
```
<?xml version="1.0" encoding="utf-8" ?>
<fileChief port="8771"
   defaultLagInterval = "30m"
   defaultQueueWarnOn = "20"
   defaultQueueWarnOff = "0"
   defaultQueueWarnInterval = "1h"
   defaultSmartRemoteRename = "true"
   defaultSaveTime = "false"
>
<!-- global parameters -->
<param name='in' value='/data/in'/>
<param name='work' value='/data/work'/>

<patterns>
<!-- simple pattern 1 -->
  <ftpDownload name="pServer1" 
    ftpServer="10.0.0.1" ftpUser="user" ftpPassword="password" ftpTimeout="60" ftpActiveMode="true" />
<!-- simple pattern 2 -->
  <ftpDownload name="pServer2" 
    ftpServer="10.0.0.2" ftpUser="user" ftpPassword="password" ftpTimeout="30" ftpActiveMode="true" />
<!-- complex pattern -->
  <ftpDownload name="pBig" pattern="pServer1"
     ftpSrcDir="{:sDir}" dstDir="{in}/{:super}"
  >
     <copy name="{:super}_to_work"  dstDir={work}/{:super} />
     <ftpUpload name="{:super}_to_backup"  pattern="pServer2"
         ftpDstDir="'/remotePath/{:super}'/yyyyMMdd"
         dstDirTimeBased="true"  gzip="true"
     />
  </ftpDownload>
</patterns>

<!-- use complex pattern 'pBig' : make chain from single agent -->
<ftpDownload name="cdr1" pattern="pBig" applyMethod="add"
    param=":sDir=/remote/src"
    fileMask="\.cdr$"
    delayBetween="10"
/>

<!-- Local move, from /data/src, to /data/dst, only files with name like 'dat*', rename files to 'cdr*', every 30 seconds.-->
<copy name="localCopy1"
   srcDir = "/data/src"  dstDir = "{out}/localCopy1dst"
   delayBetween = "30"   fileMask = "^dat" substitute="s/^dat/cdr/"
/>

<!-- Download files from SFTP server -->
<sftpDownload name="sftpDown1"
   ftpServer = "10.0.0.1" ftpUser = "user" ftpPassword = "password"
   ftpSrcDir = "/remote/dir"   dstDir = "/data/cdr"
   fileMask = "\.cdr$"
/>
<!-- OR use data from pattern -->
<sftpDownload name="sftpDown2" pattern="pServer1"
   ftpSrcDir="/remote/dir"   dstDir="/data/cdr"
   fileMask="\.cdr$"
/>


<!-- Upload files to FTP server -->
<ftpUpload name="ftpUp1" pattern="pServer1"
   srcDir = "/data/src"   ftpDstDir = "/remote/dir"
   ftpCacheDir = "@/temp"
/>

<!-- dn01. download from FTP server
     cp02. make local copy (zipped)
     up03. upload to SFTP server
-->
<sftpDownload name="dn01" pattern="pServer1"
   ftpSrcDir = "/remote/path"  dstDir = "/local/dir1"
>
   <copy name="cp02" dstDir = "/local/dir2" zip="true" />	
   <sftpUpload name="up03" pattern="pServer2" ftpDstDir = "/remote/path" />
</sftpDownload>

</fileChief>
```
###License
Apache Software License 2.0
