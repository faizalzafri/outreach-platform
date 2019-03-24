package org.outreach.outreachfeedbackserver.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.outreach.outreachfeedbackserver.entity.EventPK;
import org.outreach.outreachfeedbackserver.entity.EventSummaryEntity;
import org.outreach.outreachfeedbackserver.entity.UserRole;
import org.outreach.outreachfeedbackserver.entity.VolunteerAttended;
import org.outreach.outreachfeedbackserver.entity.VolunteerNotAttended;
import org.outreach.outreachfeedbackserver.entity.VolunteerUnregistered;
import org.outreach.outreachfeedbackserver.model.Roles;
import org.outreach.outreachfeedbackserver.model.VolunteerType;
import org.outreach.outreachfeedbackserver.repo.EventReportsRepository;
import org.outreach.outreachfeedbackserver.repo.UserRoleRepository;
import org.outreach.outreachfeedbackserver.repo.VolunteerAttendedRepo;
import org.outreach.outreachfeedbackserver.repo.VolunteerNotAttendedRepo;
import org.outreach.outreachfeedbackserver.repo.VolunteerUnregisteredRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoadExcelUtil {

	@Autowired
	private VolunteerAttendedRepo volunteerAttendedRepo;

	@Autowired
	private VolunteerNotAttendedRepo volunteerNotAttendedRepo;

	@Autowired
	private VolunteerUnregisteredRepo volunteerUnregisteredRepo;

	@Autowired
	private EventReportsRepository eventSummaryRepo;

	@Autowired
	private UserRoleRepository userRoleRepository;

	public void readExcel(Path path) throws ParseException {

		// >>Check which file to load in which
		String fileName = path.getFileName().toString();
		VolunteerType volunteerType = null;
		if (fileName.equals("Volunteer_Enrollment Details_Not_Attend.xlsx")) {
			volunteerType = VolunteerType.ABSENT;
		} else if (fileName.equals("OutReach Event Information.xlsx")) {
			volunteerType = VolunteerType.ATTENDED;
		} else if (fileName.equals("Volunteer_Enrollment Details_Unregistered.xlsx")) {
			volunteerType = VolunteerType.UNREGISTERED;
		} else if (fileName.equals("Outreach Events Summary.xlsx")) {

		} else {
			return;
		}

		FileInputStream fis = null;
		XSSFWorkbook myWorkBook = null;
		try {
			fis = new FileInputStream(path.toFile());

			myWorkBook = new XSSFWorkbook(fis);

			XSSFSheet mySheet = myWorkBook.getSheetAt(0);

			Iterator<Row> rowIterator = mySheet.iterator();

			if (rowIterator.hasNext()) {
				rowIterator.next();
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

			if (VolunteerType.ATTENDED == volunteerType) {
				List<VolunteerAttended> eventInfoList = new ArrayList<>();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					VolunteerAttended eventInfoEntity = getAttendedRecord(dateFormat, row);
					eventInfoEntity.setEmailStatus("I");
					eventInfoList.add(eventInfoEntity);
				}

				volunteerAttendedRepo.saveAll(eventInfoList);
			} else if (VolunteerType.ABSENT == volunteerType) {
				List<VolunteerNotAttended> eventInfoList = new ArrayList<>();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					VolunteerNotAttended eventInfoEntity = getAbsentRecord(dateFormat, row);
					eventInfoEntity.setEmailStatus("I");
					eventInfoList.add(eventInfoEntity);
				}
				volunteerNotAttendedRepo.saveAll(eventInfoList);
			} else if (VolunteerType.UNREGISTERED == volunteerType) {
				List<VolunteerUnregistered> eventInfoList = new ArrayList<>();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					VolunteerUnregistered eventInfoEntity = getUnregisteredRecord(dateFormat, row);
					eventInfoEntity.setEmailStatus("I");
					eventInfoList.add(eventInfoEntity);
				}
				volunteerUnregisteredRepo.saveAll(eventInfoList);
			} else if (volunteerType == null) {
				List<EventSummaryEntity> eventInfoList = new ArrayList<>();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					EventSummaryEntity eventInfoEntity = getEventInfoRecord(row);
					eventInfoList.add(eventInfoEntity);
				}
				eventSummaryRepo.saveAll(eventInfoList);
			}
		} catch (IOException ie) {
			ie.printStackTrace();
		} finally {
			if (myWorkBook != null) {
				try {
					myWorkBook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private EventSummaryEntity getEventInfoRecord(Row row) {
		EventSummaryEntity eventSummaryEntity = new EventSummaryEntity();
		eventSummaryEntity.setEventId(row.getCell(0).getStringCellValue());
		row.getCell(18).setCellType(CellType.STRING);
		eventSummaryEntity.setPocId(row.getCell(18).getStringCellValue());
		eventSummaryEntity.setPocName(row.getCell(19).getStringCellValue());
		return eventSummaryEntity;
	}

	private VolunteerUnregistered getUnregisteredRecord(SimpleDateFormat dateFormat, Row row) throws ParseException {
		VolunteerUnregistered eventInfoEntity = new VolunteerUnregistered();
		row.getCell(5).setCellType(CellType.STRING);
		eventInfoEntity
				.setEventPK(new EventPK(row.getCell(0).getStringCellValue(), row.getCell(5).getStringCellValue() + ""));
		eventInfoEntity.setBaseLocation(row.getCell(3).getStringCellValue());
		eventInfoEntity.setBeneficiaryName(row.getCell(2).getStringCellValue());
		eventInfoEntity.setEventDate(dateFormat.parse(row.getCell(4).getStringCellValue()));
		eventInfoEntity.setEventName(row.getCell(1).getStringCellValue());
		return eventInfoEntity;
	}

	private VolunteerNotAttended getAbsentRecord(SimpleDateFormat dateFormat, Row row) throws ParseException {
		VolunteerNotAttended eventInfoEntity = new VolunteerNotAttended();
		row.getCell(5).setCellType(CellType.STRING);
		eventInfoEntity
				.setEventPK(new EventPK(row.getCell(0).getStringCellValue(), row.getCell(5).getStringCellValue() + ""));
		eventInfoEntity.setBaseLocation(row.getCell(3).getStringCellValue());
		eventInfoEntity.setBeneficiaryName(row.getCell(2).getStringCellValue());
		eventInfoEntity.setEventDate(dateFormat.parse(row.getCell(4).getStringCellValue()));
		eventInfoEntity.setEventName(row.getCell(1).getStringCellValue());
		return eventInfoEntity;
	}

	private VolunteerAttended getAttendedRecord(SimpleDateFormat dateFormat, Row row) throws ParseException {
		VolunteerAttended eventInfoEntity = new VolunteerAttended();
		row.getCell(7).setCellType(CellType.STRING);
		eventInfoEntity
				.setEventPK(new EventPK(row.getCell(0).getStringCellValue(), row.getCell(7).getStringCellValue() + ""));

		eventInfoEntity.setBaseLocation(row.getCell(1).getStringCellValue());
		eventInfoEntity.setBeneficiaryName(row.getCell(2).getStringCellValue());
		// eventInfoEntity.setEmployeeName(row.getCell(8).getStringCellValue());
		eventInfoEntity.setEventDate(dateFormat.parse(row.getCell(6).getStringCellValue()));
		eventInfoEntity.setEventName(row.getCell(4).getStringCellValue());
		return eventInfoEntity;
	}

	public void savePmoDetails(String filePath) throws IOException {

		FileInputStream fis = null;
		XSSFWorkbook myWorkBook = null;
		List<UserRole> pmoList = new ArrayList<>();

		try {
			fis = new FileInputStream(filePath);
			myWorkBook = new XSSFWorkbook(fis);
			Row row = null;
			XSSFSheet mySheet = myWorkBook.getSheetAt(0);

			Iterator<Row> rowIterator = mySheet.iterator();

			if (rowIterator.hasNext()) {
				rowIterator.next();
			}

			while (rowIterator.hasNext()) {
				row = rowIterator.next();
				row.getCell(0).setCellType(CellType.STRING);
				pmoList.add(new UserRole(row.getCell(0).getStringCellValue(), Roles.PMO));
			}
		} catch (IOException ie) {
			ie.printStackTrace();
		} finally {
			if (myWorkBook != null) {
				try {
					myWorkBook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		userRoleRepository.saveAll(pmoList);
		Files.delete(Paths.get(filePath));

	}

	/*public List<EventInformationEntity> findNotMatchingRecords(List<EventInformationEntity> eventInfoList,
			VolunteerType volunteerType) {
		if (volunteerType == VolunteerType.ATTENDED) {
			List<VolunteerAttended> allRecords=new ArrayList<>();
			 volunteerAttendedRepo.findAll().forEach(allRecords::add);
			 allRecords.stream().forEach(eie -> {
				 
			 });
		} else if (volunteerType == VolunteerType.UNREGISTERED) {
			List<VolunteerUnregistered> allRecords = volunteerUnregisteredRepo.findAll();
		} else if (volunteerType == VolunteerType.ABSENT) {
			List<VolunteerNotAttended> allRecords = volunteerNotAttendedRepo.findAll();
		}
		return null;
	}*/

}
