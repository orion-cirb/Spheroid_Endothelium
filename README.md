* **Developed for:** Sandra
* **Team:** Prochiantz
* **Date:** January 2023
* **Software:** Fiji


### Images description

3D images taken with a x25 objective

3 channels:
  1. *405:* Nuclei
  2. *488:* Lamin (not mandatory)
  3. *561:* ORF1P

### Plugin description

* Detect nuclei with Cellpose
* Compute nuclei 3D shape descriptors
* In lamin channel (if provided), measure nuclei intensity
* In ORF1P channel, get cytoplasm mask 
* Measure nuclei and cytoplasm intensity

### Dependencies

* **3DImageSuite** Fiji plugin
* **CLIJ** Fiji plugin
* **Omnipose** conda environment + *cyto2* model

### Version history

Version 1 released on January 23, 2023.
