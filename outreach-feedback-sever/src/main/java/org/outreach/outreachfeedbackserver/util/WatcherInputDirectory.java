package org.outreach.outreachfeedbackserver.util;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WatcherInputDirectory {

	@Autowired
	LoadExcelUtil loadExcelUtil;
	
	public void watch() throws ParseException {
		
		System.out.println("Load Excel Util ::"+ loadExcelUtil);
		try {
			// Create a WatchService and register the logDir path with the
			// WatchService for ENTRY_CREATE.
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path logDir = Paths.get("C:\\Input Test Files");
			logDir.register(watcher, ENTRY_CREATE,ENTRY_MODIFY);

			while (true) {
				WatchKey key;
				try {
					key = watcher.take();
				} catch (InterruptedException e) {
					return;
				}

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();

					if (ENTRY_CREATE.equals(kind)) {
						// Get the name of created file.
						WatchEvent<Path> ev = cast(event);
						Path filename = ev.context();

						
						Path dir = (Path)key.watchable();
						Path fullPath = dir.resolve(ev.context());
						
						System.out.printf("A new file %s was created.%n", filename.getFileName());
					
					/*	if(isValidFile(filename.toString()))
						loadExcelUtil.readExcel(fullPath);*/

					} else if (ENTRY_MODIFY.equals(kind)) {
						WatchEvent<Path> ev = cast(event);
						Path filename = ev.context();
						Path dir = (Path)key.watchable();
						Path fullPath = dir.resolve(ev.context());
						System.out.printf("A new file %s was created.%n", filename.getFileName());
						System.out.println("Entry was modified on log dir.");
						if(isValidFile(filename.toString()))
						loadExcelUtil.readExcel(fullPath);
						
					} else if (ENTRY_DELETE.equals(kind)) {
						System.out.println("Entry was deleted from log dir.");
					}
				}
				key.reset();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}
	
	public boolean isValidFile(String fileName) {
		if (fileName.equals("Volunteer_Enrollment Details_Not_Attend.xlsx")) {
			return true;
		} else if (fileName.equals("OutReach Event Information.xlsx")) {
			return true;
		} else if (fileName.equals("Volunteer_Enrollment Details_Unregistered.xlsx")) {
			return true;
		}else if (fileName.equals("Outreach Events Summary.xlsx")) {
			return true;
		}
		return false;
	}
}
