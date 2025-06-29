let currentUser = "";
let currentPath = "/";

// Entrar al Drive (login)
function enterDrive() {
  const username = document.getElementById("username").value.trim();
  if (!username) return alert("Ingrese un nombre de usuario");

  sendCommand("LOGIN", [username])
    .then(response => {
      console.log(" Respuesta del backend:", response);
      if (response.success) {
        document.getElementById("driveUI").style.display = "block";
        document.getElementById("userInput").style.display = "none";
        loadDirectory();
      } else {
        alert(response.message);
      }
    });
}

// Listar contenido del directorio actual
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
            div.onclick = () => {
              currentPath += folderName + "/";
              sendCommand("CHANGE_DIR", [folderName])
                .then(resp => {
                  if (resp.success) loadDirectory();
                  else alert(resp.message);
                });
            };
          } else if (line.startsWith("[FILE]")) {
            const fileName = line.replace("[FILE]", "").split("(")[0].trim();
            div.className = "file";
            div.innerText = fileName;
            div.onclick = () => {
              sendCommand("VIEW_FILE", [fileName])
                .then(resp => {
                  if (resp.success) {
                    alert("Contenido de " + fileName + ":\n\n" + resp.data);
                  } else {
                    alert(resp.message);
                  }
                });
            };
          }
          fileView.appendChild(div);
        });
      } else {
        if (response.success) {
          fileView.innerText = "(Directorio vacío)";
        } else {
          fileView.innerText = response.message || "Error al listar contenido.";
        }

      }
    });
}

// Modal
let modalAction = "";
function createFolder() {
  modalAction = "folder";
  openModal("Crear Directorio");
}
function createFile() {
  modalAction = "file";
  openModal("Crear Archivo");
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

// Función genérica para enviar comandos al backend
function sendCommand(type, parameters) {
  const command = {
    type: type,
    parameters: parameters
  };

  return fetch('http://127.0.0.1:3000/sendCommand', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(command)
  })
    .then(response => response.json())
    .catch(err => {
      console.error("Error de red o de Node.js:", err);
      return { success: false, message: "Error de conexión al backend" };
    });
}
