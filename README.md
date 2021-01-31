# PDF API

This is a spring boot api for working with PDF files. It is based on [IText library][itext].

The following operations are currently supported:
  - Merge two PDFs
  - Split a PDF
  - Convert an image file to PDF

## Merge

**URL**:
```
POST /pdfapi/merge
```

**Form-data Params**: ```file1``` and ```file2```. 

It merges ```file1``` and ```file2``` into a new PDF.

## Split

**URL**:
```
POST /pdfapi/split
```
 
 **Form-data Params**: ```file``` and ```maxPageCount```. 
 
 It creates a new PDF per ```maxPageCount``` pages from ```file```.
 
 ## Convert Image To PDF
 
 **URL**:
```
POST /pdfapi/convertImageToPDF
```
 
 **Form-data Params**: ```file```.
 
 It converts ```file``` into a PDF.
 
   [itext]: <http://itextpdf.com/en>
