# PDF API

This is a spring boot api for working with PDF files. It is based on [IText library][itext].

The following operations are currently supported:
  - Merge multiple PDFs
  - Split a PDF
  - Extract pages from a PDF
  - Remove pages form a PDF  
  - Convert multiple image files to PDFs

## Merge

**URL**:
```
POST /pdfapi/merge
```

**Form-data Params**: ```file```. 

It merges multiples files tagged with ```file``` into a new PDF.

## Split

**URL**:
```
POST /pdfapi/split
```
 
**Form-data Params**: ```file``` and ```maxPageCount```. 
 
It creates a new PDF per ```maxPageCount``` pages from ```file```.

## Extract

**URL**
```
POST /pdfapi/extract
```

**Form-data params**: ```file```, ```startPage``` and ```endPage```.

It creates a new PDF containing the pages from ```startPage``` to ```endPage``` from ```file```.

## Remove

**URL**
```
POST /pdfapi/remove
```

**Form-data params**: ```file``` and ```page```.

It creates a copy of ```file``` and removes each page number tagged with ```page```. 

## Convert Image To PDF
 
**URL**:
```
POST /pdfapi/convertImageToPDF
```
 
**Form-data Params**: ```file```.
 
It converts each image file tagged with ```file``` into a PDF.
 
   [itext]: <http://itextpdf.com/en>
