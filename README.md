# PV_PNN

* **Developed for:** David
* **Team:** Prochiantz
* **Date:** November 2022
* **Software:** Fiji



### Images description

3D images taken with a x60 objective

4 channels:
  1. *Alexa Fluor 647:* PV cells
  2. *Alexa Fluor 546:* PNN cells
  3. *EGFP:* Gamma-H2AX
  4. *Hoechst 33342:* DAPI nuclei

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

