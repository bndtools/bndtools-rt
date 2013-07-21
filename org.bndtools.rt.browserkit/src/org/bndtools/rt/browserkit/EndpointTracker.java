package org.bndtools.rt.browserkit;

import java.util.concurrent.atomic.AtomicInteger;

import org.bndtools.rt.browserkit.api.BrowserKitConstants;
import org.bndtools.service.endpoint.Endpoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class EndpointTracker extends ServiceTracker {

	private final Display display;
	private final AtomicInteger counter = new AtomicInteger(0); 

	public EndpointTracker(BundleContext context, Display display) {
		super(context, createFilter(), null);
		this.display = display;
	}
	
	private static Filter createFilter() {
		try {
			return FrameworkUtil.createFilter(String.format("(&(objectClass=%s)(|(uri=http:*)(uri=https:*)))", Endpoint.class.getName()));
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Object addingService(final ServiceReference reference) {
		final String uriStr = (String) reference.getProperty(Endpoint.URI);
		if (uriStr == null)
			return null;
		
		if (display.isDisposed())
			return null;
		
		final Shell shell = new Shell(display);
		display.asyncExec(new Runnable() {
			public void run() {
				shell.setLayout(new FillLayout());
				
				String title = (String) reference.getProperty(BrowserKitConstants.WINDOW_TITLE);
				if (title == null)
					title = "SWT Kit";
				
				shell.setText(title);
				final Browser browser;
				try {
					browser = new Browser(shell, SWT.WEBKIT);
				} catch (SWTError e) {
					System.out.println("Could not instantiate Browser: " + e.getMessage());
					display.dispose();
					return;
				}
				shell.addShellListener(new ShellAdapter() {
					@Override
					public void shellClosed(ShellEvent e) {
						shell.dispose();
						int newcount = counter.decrementAndGet();
						System.out.printf("Shell count: %d%n", newcount);
						if (newcount == 0)
							display.dispose();
					}
				});
				shell.open();
				browser.setUrl(uriStr);
			}
		});

		int newcount = counter.incrementAndGet();
		System.out.printf("Shell count: %d%n", newcount);
		
		return shell;
	}
	
	@Override
	public void removedService(ServiceReference reference, Object service) {
		final Shell shell = (Shell) service;
		if (display.isDisposed())
			return;

		display.asyncExec(new Runnable() {
			public void run() {
				if (shell.isDisposed())
					return;

				shell.dispose();
				
				int newcount = counter.decrementAndGet();
				System.out.printf("Shell count: %d%n", newcount);
				if (newcount == 0)
					display.dispose();
			}
		});
	}
}
