/*******************************************************************************
 * Copyright (c) 2012 Paremus Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Paremus Ltd - initial API and implementation
 ******************************************************************************/
package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainTest_01 {

	public static void main(String args[]) throws InterruptedException, IOException {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Main Test 01 Exiting from shutodwn hook");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		File test = new File("test");
		FileWriter t = new FileWriter(test);
		t.write('A');
		t.close();
		while(true) {
			System.out.println("I am here");
			Thread.sleep(1000);
		}
	}
}
