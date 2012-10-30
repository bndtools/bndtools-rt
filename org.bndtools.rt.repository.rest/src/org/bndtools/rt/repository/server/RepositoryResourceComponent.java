package org.bndtools.rt.repository.server;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.osgi.framework.Constants;
import org.osgi.service.provisioning.ProvisioningService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.deployer.repository.LocalIndexedRepo;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@Path("/")
@Component(
		name = "org.bndtools.rt.repository",
		provide = Object.class,
		immediate = true,
		properties = "osgi.rest.alias=/repo",
		designateFactory = RepositoryResourceComponent.Config.class)
public class RepositoryResourceComponent {
	
	interface Config {
		@Meta.AD
		String local();
	}

	private final LocalIndexedRepo repo = new LocalIndexedRepo();
	private final JsonFactory jsonFactory = new JsonFactory();
	
	private File storageDir;
	
	@Activate
	void activate(Map<String, String> configProps) throws Exception {
		repo.setProperties(configProps);
		storageDir = repo.getRoot().getCanonicalFile();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response list(@Context UriInfo uriInfo) throws Exception {
		String query = uriInfo.getRequestUri().getQuery();
		if (query == null || query.isEmpty())
			throw new IllegalArgumentException("Empty query not allowed");
		
		List<String> list = repo.list(query);
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path("{bsn}");
		
		StringWriter writer = new StringWriter();
		JsonGenerator generator = jsonFactory.createJsonGenerator(writer);
		generator.writeStartArray();
		for (String bsn : list) {
			generator.writeStartObject();
			generator.writeStringField("bsn", bsn);
			generator.writeStringField("href", uriBuilder.build(bsn).toString());
			generator.writeEndObject();
		}
		generator.writeEndArray();
		generator.close();
		
		return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
	}
	
	@GET
	@Path("index")
	@Produces(MediaType.APPLICATION_XML)
	public Response getIndex(@Context HttpHeaders headers) throws Exception {
		List<URI> locations = repo.getIndexLocations();
		if (locations.isEmpty())
			return Response.serverError().entity("No index available").build();
		
		Response response;
		URI location = locations.get(0);
		if ("file".equals(location.getScheme())) {
			File file = new File(location);
			
			@SuppressWarnings("resource")
			FileInputStream indexStream = new FileInputStream(file);
			InputStream bufferedStream = indexStream.markSupported() ? indexStream : new BufferedInputStream(indexStream);
			
			InputStream responseStream;
			if (isGZip(bufferedStream)) {
				responseStream = new GZIPInputStream(indexStream);
			} else {
				responseStream = bufferedStream;
			}
			response = Response.ok(responseStream, MediaType.APPLICATION_XML_TYPE).build();
		} else {
			response = Response.status(Status.SEE_OTHER).location(location).build();
		}
		return response;
	}
	
	@GET
	@Path("{bsn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listVersions(@Context UriInfo uriInfo, @PathParam("bsn") String bsn) throws Exception {
		SortedSet<Version> versions = repo.versions(bsn);
		
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().path("{filename}");
		String prefix = new File(storageDir, bsn).getAbsolutePath() + File.separatorChar;
		
		StringWriter writer = new StringWriter();
		JsonGenerator generator = jsonFactory.createJsonGenerator(writer);
		generator.writeStartArray();
		for (Version version : versions) {
			ResourceHandle handle = repo.getHandle(bsn, version.toString(), Strategy.EXACT, null);
			if (Location.local.equals(handle.getLocation())) {
				String bundlePath = handle.request().getCanonicalPath();
				if (bundlePath.startsWith(prefix)) {
					bundlePath = bundlePath.substring(prefix.length());
					URI bundleUri = uriBuilder.build(bundlePath);
					
					generator.writeStartObject();
					generator.writeStringField("bsn", bsn);
					generator.writeStringField("version", version.toString());
					generator.writeStringField("href", bundleUri.toString());
					generator.writeEndObject();
				}
			}
		}
		generator.writeEndArray();
		generator.close();
		
		return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
	}

	
	private static boolean isGZip(InputStream bufferedStream) throws IOException {
		assert bufferedStream.markSupported();
		
		bufferedStream.mark(2);
		int magic = readUShort(bufferedStream);
		bufferedStream.reset();
		
		return magic == GZIPInputStream.GZIP_MAGIC;
	}
	
	private static int readUShort(InputStream in) throws IOException {
		int b = readUByte(in);
		return (readUByte(in) << 8) | b;
	}

	private static int readUByte(InputStream in) throws IOException {
		int b = in.read();
		if (b == -1) {
			throw new EOFException();
		}
		if (b < -1 || b > 255) {
			// Report on this.in, not argument in; see read{Header, Trailer}.
			throw new IOException(in.getClass().getName() + ".read() returned value out of range -1..255: " + b);
		}
		return b;
	}
		
	
	@POST
	@Consumes({ MediaType.APPLICATION_OCTET_STREAM, ProvisioningService.MIME_BUNDLE })
	public Response uploadBundle(@Context UriInfo uriInfo, InputStream stream) throws Exception {
		if (!repo.canWrite())
			return Response.status(Status.BAD_REQUEST).entity("Repository is not writeable").build();
		
		InputStream bufferedStream = stream.markSupported() ? stream : new BufferedInputStream(stream);
		Manifest manifest = readManifest(bufferedStream);
		
		Attributes mainAttribs = manifest.getMainAttributes();
		String bsn = mainAttribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (bsn == null)
			return Response.status(Status.BAD_REQUEST).entity("not a bundle").build();
		
		PutResult result = repo.put(bufferedStream, RepositoryPlugin.DEFAULTOPTIONS);
		if ("file".equals(result.artifact.getScheme())) {
			File bundleFile = new File(result.artifact).getCanonicalFile();
			String prefix = storageDir.getAbsolutePath() + File.separatorChar;
			if (!bundleFile.getAbsolutePath().startsWith(prefix))
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("bundle not created inside repository storage location").build();
			
			String relativePath = bundleFile.getAbsolutePath().substring(prefix.length());
			URI location = uriInfo.getAbsolutePathBuilder().path(relativePath).build();
			return Response.created(location).build();
		} else {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("bundle not created as a local file").build();
		}
	}
	
	@GET
	@Path("{bsn}/{filename}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getBundle(@PathParam("bsn") String bsn, @PathParam("filename") String filename) throws Exception {
		File bundleFile = new File(storageDir, bsn + File.separatorChar + filename);
		if (!bundleFile.isFile())
			return Response.status(Status.NOT_FOUND).build();
		
		return Response.ok(new FileInputStream(bundleFile), MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
	}
	
	private static Manifest readManifest(InputStream stream) throws IOException {
		if (!stream.markSupported())
			throw new IOException("Stream must support mark/reset");
		
		stream.mark(100000);
		try {
			@SuppressWarnings("resource")
			JarInputStream jarStream = new JarInputStream(stream);
			return jarStream.getManifest();
		} finally {
			stream.reset();
		}
	}
	
	
}
