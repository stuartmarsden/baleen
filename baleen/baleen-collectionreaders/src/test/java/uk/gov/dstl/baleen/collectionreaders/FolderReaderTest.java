//Dstl (c) Crown Copyright 2015
package uk.gov.dstl.baleen.collectionreaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;

import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.gov.dstl.baleen.uima.BaleenCollectionReader;

public class FolderReaderTest {
	private static final String TEST3_FILE = "test3.txt";
	private static final String TEST2_FILE = "test2.txt";
	private static final String TEXT1_FILE = "test1.txt";
	private static final String DIR = "baleen-test";
	File inputDir;
	JCas jCas;

	@Before
	public void beforeTest() throws Exception{
		inputDir = Files.createTempDirectory(DIR).toFile();
		if(jCas == null){
			jCas = JCasFactory.createJCas();
		}else{
			jCas.reset();
		}
	}

	@After
	public void afterTest() throws IOException{
		String[] entries = inputDir.list();
		for(String s : entries){
			File currentFile = new File(inputDir.getPath(), s);
			currentFile.delete();
		}
		inputDir.delete();
	}

	@Test
	public void testCreateFileDefaultDirectory() throws Exception{
		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class);

		assertTrue(bcr.doHasNext());	//There will be files in the current directory, so we can just check that it's picked them up.
		bcr.getNext(jCas.getCas());
		assertTrue(getSource(jCas).contains(System.getProperty("user.dir")));
		
		bcr.close();
	}
	
	@Test
	public void testCreateFile() throws Exception{
		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()});

		assertFalse(bcr.doHasNext());

		File f = new File(inputDir, TEXT1_FILE);
		f.createNewFile();

		//Wait for file to be written and change detected
		Thread.sleep(1000);

		assertTrue(bcr.doHasNext());

		bcr.getNext(jCas.getCas());

		assertEquals(f.getPath(), getSource(jCas));

		assertFalse(bcr.doHasNext());

		bcr.close();
	}

	@Test
	public void testMultipleDirectories() throws Exception{
		File inputDir2 = Files.createTempDirectory(DIR).toFile();

		File f11 = new File(inputDir, TEXT1_FILE);
		f11.createNewFile();

		File f21 = new File(inputDir2, TEXT1_FILE);
		f21.createNewFile();

		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath(), inputDir2.getPath()});

		File f12 = new File(inputDir, TEST2_FILE);
		f12.createNewFile();

		File f22 = new File(inputDir2, TEST2_FILE);
		f22.createNewFile();

		Thread.sleep(1000);

		assertNextSourceNotNull(bcr);
		assertNextSourceNotNull(bcr);
		assertNextSourceNotNull(bcr);
		assertNextSourceNotNull(bcr);

		f21.delete();
		f22.delete();
		inputDir2.delete();
	}

	@Test
	public void testSubDirectories() throws Exception{
		File subdir = new File(inputDir, "subdir");
		subdir.mkdir();

		File f1 = new File(subdir, TEXT1_FILE);
		f1.createNewFile();

		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()});

		assertNextSourceNotNull(bcr);

		File f2 = new File(subdir, TEST2_FILE);
		f2.createNewFile();

		Thread.sleep(1000);

		assertNextSourceNotNull(bcr);

		bcr.close();

		f1.delete();
		f2.delete();
		subdir.delete();
	}
	
	@Test
	public void testSubDirectoriesNonRecursive() throws Exception{
		File subdir = new File(inputDir, "subdir");
		subdir.mkdir();

		File f1 = new File(subdir, TEXT1_FILE);
		f1.createNewFile();
		
		File f2 = new File(inputDir, TEST2_FILE);
		f2.createNewFile();

		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()}, FolderReader.PARAM_RECURSIVE, false);

		assertTrue(bcr.hasNext());
		bcr.getNext(jCas.getCas());
		assertEquals(f2.getPath(), getSource(jCas));
		
		jCas.reset();
		
		File f3 = new File(inputDir, TEST3_FILE);
		f3.createNewFile();
		
		Thread.sleep(1000);
		
		assertTrue(bcr.hasNext());
		bcr.getNext(jCas.getCas());
		assertEquals(f3.getPath(), getSource(jCas));
		
		bcr.close();

		f1.delete();
		f2.delete();
		subdir.delete();
	}

	@Test
	public void testModifiedFile() throws Exception{
		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()}, FolderReader.PARAM_REPROCESS_ON_MODIFY, true);

		assertFalse(bcr.doHasNext());

		File f = new File(inputDir, TEXT1_FILE);
		f.createNewFile();

		//Wait for file to be written and change detected
		Thread.sleep(1000);

		assertTrue(bcr.doHasNext());

		bcr.getNext(jCas.getCas());
		assertEquals(f.getPath(), getSource(jCas));

		jCas.reset();

		//Modify file
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
		writer.write("Test");
		writer.close();

		Thread.sleep(1000);

		assertTrue(bcr.doHasNext());

		bcr.getNext(jCas.getCas());

		assertEquals(f.getPath(), getSource(jCas));
		assertEquals("Test", jCas.getDocumentText().trim());

		assertFalse(bcr.doHasNext());

		bcr.close();
	}

	@Test
	public void testDeleteFile() throws Exception{
		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()});

		assertFalse(bcr.doHasNext());

		File f = new File(inputDir, TEXT1_FILE);
		f.createNewFile();

		//Wait for file to be written and change detected
		Thread.sleep(1000);

		f.delete();

		//Wait for file to be written and change detected
		Thread.sleep(1000);

		assertFalse(bcr.doHasNext());

		bcr.close();
	}

	@Test
	public void testExistingFiles() throws Exception{
		File f1 = new File(inputDir, TEXT1_FILE);
		f1.createNewFile();
		File f2 = new File(inputDir, TEST2_FILE);
		f2.createNewFile();

		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()});

		assertNextSourceNotNull(bcr);
		assertNextSourceNotNull(bcr);
		assertFalse(bcr.doHasNext());

		bcr.close();
	}
	
	@Test
	public void testExtensionFilter() throws Exception{
		File f1 = new File(inputDir, TEXT1_FILE);
		f1.createNewFile();
		File f2 = new File(inputDir, "test2.log");
		f2.createNewFile();
		File f3 = new File(inputDir, "test3.TXT");
		f3.createNewFile();

		BaleenCollectionReader bcr = (BaleenCollectionReader) CollectionReaderFactory.createReader(FolderReader.class, FolderReader.PARAM_FOLDERS, new String[]{inputDir.getPath()}, FolderReader.PARAM_ALLOWED_EXTENSIONS, new String[]{"txt"});

		assertNextSourceNotNull(bcr);
		assertNextSourceNotNull(bcr);
		assertFalse(bcr.doHasNext());

		bcr.close();
	}
	
	private void assertNextSourceNotNull(BaleenCollectionReader bcr) throws Exception{
		assertTrue(bcr.doHasNext());
		bcr.getNext(jCas.getCas());
		assertNotNull(getSource(jCas));
		jCas.reset();
	}
	
	private String getSource(JCas jCas){
		DocumentAnnotation doc = (DocumentAnnotation) jCas.getDocumentAnnotationFs();
		return doc.getSourceUri();
	}
}