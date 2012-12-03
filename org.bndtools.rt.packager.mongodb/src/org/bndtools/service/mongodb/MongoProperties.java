/*******************************************************************************
 * Copyright (c) 2012 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.service.mongodb;

import org.bndtools.service.packager.*;

import aQute.bnd.annotation.metatype.Meta;


/**
 * <pre>
 *   -h [ --help ]               show this usage information
 *   --version                   show version information
 *   -f [ --config ] arg         configuration file specifying additional options
 *   -v [ --verbose ]            be more verbose (include multiple times for more 
 *                               verbosity e.g. -vvvvv)
 *   --quiet                     quieter output
 *   --port arg                  specify port number
 *   --bind_ip arg               comma separated list of ip addresses to listen on
 *                               - all local ips by default
 *   --maxConns arg              max number of simultaneous connections
 *   --objcheck                  inspect client data for validity on receipt
 *   --logpath arg               log file to send write to instead of stdout - has
 *                               to be a file, not directory
 *   --logappend                 append to logpath instead of over-writing
 *   --pidfilepath arg           full path to pidfile (if not set, no pidfile is 
 *                               created)
 *   --keyFile arg               private key for cluster authentication (only for 
 *                               replica sets)
 *   --nounixsocket              disable listening on unix sockets
 *   --unixSocketPrefix arg      alternative directory for UNIX domain sockets 
 *                               (defaults to /tmp)
 *   --fork                      fork server process
 *   --auth                      run with security
 *   --cpu                       periodically show cpu and iowait utilization
 *   --dbpath arg                directory for datafiles
 *   --diaglog arg               0=off 1=W 2=R 3=both 7=W+some reads
 *   --directoryperdb            each database will be stored in a separate 
 *                               directory
 *   --journal                   enable journaling
 *   --journalOptions arg        journal diagnostic options
 *   --journalCommitInterval arg how often to group/batch commit (ms)
 *   --ipv6                      enable IPv6 support (disabled by default)
 *   --jsonp                     allow JSONP access via http (has security 
 *                               implications)
 *   --noauth                    run without security
 *   --nohttpinterface           disable http interface
 *   --nojournal                 disable journaling (journaling is on by default 
 *                               for 64 bit)
 *   --noprealloc                disable data file preallocation - will often hurt
 *                               performance
 *   --noscripting               disable scripting engine
 *   --notablescan               do not allow table scans
 *   --nssize arg (=16)          .ns file size (in MB) for new databases
 *   --profile arg               0=off 1=slow, 2=all
 *   --quota                     limits each database to a certain number of files
 *                               (8 default)
 *   --quotaFiles arg            number of files allower per db, requires --quota
 *   --rest                      turn on simple rest api
 *   --repair                    run repair on all dbs
 *   --repairpath arg            root directory for repair files - defaults to 
 *                               dbpath
 *   --slowms arg (=100)         value of slow for profile and console log
 *   --smallfiles                use a smaller default file size
 *   --syncdelay arg (=60)       seconds between disk syncs (0=never, but not 
 *                               recommended)
 *   --sysinfo                   print some diagnostic system information
 *   --upgrade                   upgrade db if needed
 * 
 * Replication options:
 *   --fastsync            indicate that this instance is starting from a dbpath 
 *                         snapshot of the repl peer
 *   --oplogSize arg       size limit (in MB) for op log
 * 
 * Master/slave options:
 *   --master              master mode
 *   --slave               slave mode
 *   --source arg          when slave: specify master as <server:port>
 *   --only arg            when slave: specify a single database to replicate
 *   --slavedelay arg      specify delay (in seconds) to be used when applying 
 *                         master ops to slave
 *   --autoresync          automatically resync if slave data is stale
 * 
 * Replica set options:
 *   --replSet arg         arg is <setname>[/<optionalseedhostlist>]
 * 
 * Sharding options:
 *   --configsvr           declare this is a config db of a cluster; default port 
 *                         27019; default dir /data/configdb
 *   --shardsvr            declare this is a shard db of a cluster; default port 
 *                         27018
 *   --noMoveParanoia      turn off paranoid saving of data for moveChunk.  this 
 *                         is on by default for now, but default will switch
 * 
 * </pre>
 */

public interface MongoProperties extends PackagerStandardProperties {
	
	boolean quiet();
	@Meta.AD(required = false, deflt = "27017")
	int port();
	String[] bind_ip();
	String dbpath();

}
