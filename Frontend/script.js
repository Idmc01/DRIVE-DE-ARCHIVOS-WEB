let currentUser = null;
let currentPath = "/";

// ----------------------
// Función para limpiar rutas
// ----------------------
function sanitizePath(inputPath) {
  if (!inputPath) return "/";
  let path = inputPath.trim();
  if (!path.startsWith("/")) path = "/" + path;
  path = path.replace(/\s+/g, "");
  path = path.replace(/\/{2,}/g, "/");
  return path;
}

// ----------------------
// LOGIN
// ----------------------
function enterDrive() {
  const username = document.getElementById("username").value.trim();
  if (!username) return alert("Ingrese un nombre de usuario");

  sendCommand("LOGIN", [username])
    .then(response => {
      if (response.success) {
        currentUser = username;
        document.getElementById("driveUI").style.display = "block";
        document.getElementById("userInput").style.display = "none";
        loadDirectory();
      } else {
        alert(response.message);
      }
    });
}

// ----------------------
// Listar archivos y carpetas
// ----------------------
function loadDirectory() {
  document.getElementById("currentPath").innerText = currentPath;
  const fileView = document.getElementById("fileView");
  fileView.innerHTML = "";

  sendCommand("LIST_DIR", [])
    .then(response => {
      if (response.success && response.data) {
        const lines = response.data.split("\n");
        lines.forEach(line => {
          if (line.trim() === "") return;
          const div = document.createElement("div");

          if (line.startsWith("[DIR]")) {
            const folderName = line.replace("[DIR]", "").trim();
            div.className = "folder";
            div.innerText = folderName;
            div.onclick = () => changeDir(folderName);
          } else if (line.startsWith("[FILE]")) {
            const fileName = line.replace("[FILE]", "").split("(")[0].trim();
            div.className = "file";
            div.innerText = fileName;
            div.onclick = () => {
              if (confirm(`¿Ver contenido o descargar "${fileName}"?\nAceptar: Ver\nCancelar: Descargar`)) {
                viewFile(fileName);
              } else {
                downloadFile(fileName);
              }
            };
          }
          fileView.appendChild(div);
        });
      } else {
        fileView.innerText = response.message || "(Vacío)";
      }
    });
}

// ----------------------
// Cambiar de directorio
// ----------------------
function changeDir(folderName) {
  currentPath += folderName + "/";
  sendCommand("CHANGE_DIR", [folderName])
    .then(response => {
      if (response.success) loadDirectory();
      else alert(response.message);
    });
}

function goBack() {
  sendCommand("CHANGE_DIR", [".."])
    .then(response => {
      if (response.success) {
        const parts = currentPath.split("/");
        parts.pop(); parts.pop();
        currentPath = parts.join("/") + "/";
        if (currentPath === "//") currentPath = "/";
        loadDirectory();
      } else {
        alert(response.message);
      }
    });
}

// ----------------------
// Archivos
// ----------------------
function viewFile(filename) {
  sendCommand("VIEW_FILE", [filename])
    .then(response => {
      if (response.success) {
        alert("Contenido de " + filename + ":\n\n" + response.data);
      } else {
        alert(response.message);
      }
    });
}

function downloadFile(filename) {
  sendCommand("DOWNLOAD", [filename, currentPath])
    .then(response => {
      if (response.success) {
        const blob = new Blob([response.data], { type: 'text/plain' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename;
        link.click();
      } else {
        alert(response.message);
      }
    });
}

// ----------------------
// Crear archivo o carpeta
// ----------------------
let modalAction = "";
function createFile() {
  modalAction = "file";
  openModal("Crear Archivo");
}
function createFolder() {
  modalAction = "folder";
  openModal("Crear Directorio");
}
function openModal(title) {
  document.getElementById("modalTitle").innerText = title;
  document.getElementById("modal").classList.remove("hidden");
}
function closeModal() {
  document.getElementById("modal").classList.add("hidden");
  document.getElementById("modalInput").value = "";
  document.getElementById("modalText").value = "";
}
function confirmModal() {
  const name = document.getElementById("modalInput").value.trim();
  const content = document.getElementById("modalText").value.trim();
  closeModal();

  if (modalAction === "folder") {
    sendCommand("CREATE_DIR", [name])
      .then(response => {
        alert(response.message);
        loadDirectory();
      });
  } else {
    sendCommand("CREATE_FILE", [name, content])
      .then(response => {
        alert(response.message);
        loadDirectory();
      });
  }
}
function openDriveModal() {
  document.getElementById("driveModal").classList.remove("hidden");
  document.getElementById("driveSizeInput").value = "";
}

function closeDriveModal() {
  document.getElementById("driveModal").classList.add("hidden");
}

function confirmDriveModal() {
  const username = document.getElementById("username").value.trim();
  const sizeText = document.getElementById("driveSizeInput").value.trim();

  if (!username) {
    alert("Ingrese un nombre de usuario antes de crear el drive.");
    return;
  }

  const size = parseInt(sizeText);
  if (isNaN(size) || size <= 0) {
    alert("Ingrese un tamaño válido (número mayor a 0).");
    return;
  }

  sendCommand("CREATE_DRIVE", [username, size])
    .then(response => {
      alert(response.message);
      closeDriveModal();
    });
}


// ----------------------
// Copiar y Mover
// ----------------------
function prepareCopy() {
  const item = prompt("Elemento a copiar:");
  const dest = prompt("Ruta destino (ej: /docs/):");
  if (item && dest) {
    const cleanDest = sanitizePath(dest);
    sendCommand("COPY", [item, currentPath, cleanDest])
      .then(response => {
        alert(response.message);
        loadDirectory();
      });
  }
}

function prepareMove() {
  const item = prompt("Elemento a mover:");
  const dest = prompt("Ruta destino (ej: /docs/):");
  if (item && dest) {
    const cleanDest = sanitizePath(dest);
    sendCommand("MOVE", [item, currentPath, cleanDest])
      .then(response => {
        alert(response.message);
        loadDirectory();
      });
  }
}

// ----------------------
// Eliminar
// ----------------------
function deleteItem() {
  const item = prompt("Elemento a eliminar:");
  if (item) {
    sendCommand("DELETE", [item])
      .then(response => {
        alert(response.message);
        loadDirectory();
      });
  }
}

// ----------------------
// Compartir
// ----------------------
function shareItem() {
  const item = prompt("Elemento a compartir:");
  const destUser = prompt("Usuario destino:");
  if (item && destUser) {
    sendCommand("SHARE", [item, currentPath, destUser])
      .then(response => alert(response.message));
  }
}

// ----------------------
// Upload (LOAD)
// ----------------------
function uploadFile() {
  const fileInput = document.getElementById("fileInput");
  fileInput.onchange = () => {
    const file = fileInput.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        sendCommand("LOAD", [file.name, currentPath, reader.result])
          .then(response => {
            alert(response.message);
            loadDirectory();
          });
      };
      reader.readAsText(file);
    }
  };
  fileInput.click();
}

// ----------------------
// Envío de comando general
// ----------------------
function sendCommand(type, parameters) {
  const effectiveParams = (type === "LOGIN" || type === "CREATE_DRIVE")
    ? parameters
    : [currentUser, ...parameters];

  const command = {
    type: type,
    parameters: effectiveParams
  };

  return fetch('http://127.0.0.1:3000/sendCommand', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(command)
  })
    .then(response => response.json())
    .catch(err => {
      console.error("Error de red:", err);
      return { success: false, message: "Error de conexión al backend" };
    });
}
