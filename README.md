# PV_PNN

* **Developed for:** Tom
* **Team:** Prochiantz
* **Date:** July 2023
* **Software:** Fiji

### Images description

3D images taken on a spinning-disk with a x40 objective

3 channels:
  1. *Alexa Fluor 647:* nuclei
  2. *Alexa Fluor 546:* NeuN cells
  3. *EGFP:* ORF1p cells

### Plugin description

* Detect DAPI nuclei, NeuN and ORF1p cells with Cellpose
* Compute their colocalization
* For each cell, give various intensity measurements in its nucleus, cytoplasm and entire mask

### Dependencies

* **3DImageSuite** Fiji plugin
* **Cellpose** conda environment + *cyto2* model

### Version history

Version 1 released on July 11, 2023.

