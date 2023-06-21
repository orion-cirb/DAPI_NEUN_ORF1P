# PV_PNN

* **Developed for:** Tom
* **Team:** Prochiantz
* **Date:** June 2023
* **Software:** Fiji



### Images description

3D images taken on a spinning-disk with a x60 objective

3 channels:
  1. *Alexa Fluor 647:* nuclei
  2. *Alexa Fluor 546:* NeuN cells
  3. *EGFP:* ORF1p cells

### Plugin description

* Detect DAPI nuclei, PV and PNN cells with Cellpose
* Compute their colocalization
* Keep PV/PNN cells with a nucleus only
* Detect DAPI and Gamma-H2AX foci in each PV/PNN nucleus with Stardist

### Dependencies

* **3DImageSuite** Fiji plugin
* **CLIJ** Fiji plugin
* **Cellpose** conda environment + *cyto2*, *cyto_PV1* (homemade) and *livecell_PNN1* (homemade) models
* **StarDist** conda environment + *pmls2.zip* (homemade) model

### Version history

Version 1 released on November 17, 2022.

