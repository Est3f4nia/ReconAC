from pathlib import Path
from typing import Optional
import os
import platform


def get_path(custom_path: Optional[str] = None) -> Path:

    if custom_path:
        path = ""  # for exception handling
        try:
            path = Path(custom_path).expanduser().resolve()
            path.mkdir(parents=True, exist_ok=True)
            return path
        except PermissionError:
            print(f"[!] You don't have enough privileges to create {path} \n[-] Using fallback")
        except Exception as e:
            print(f"Error with custom path: {e} \n[-] Using fallback")

    document = _get_documents_path()
    if document and document.exists() and document.is_dir():

        try:
            path = document / "ReconAC_Results"
            path.mkdir(exist_ok=True)
            return path
        except Exception as e:
            print(f"Error creating directory in Documents: {e} \n[-] Using fallback")

    # case: fallback
    current = Path.cwd()
    if current.exists() and os.access(str(current), os.W_OK):
        return current

    return Path.home()

def _get_documents_path() -> Optional[Path]:
    
    home = Path.home()
    system = platform.system()

    if system == "Windows":
        user_profile = os.getenv("USERPROFILE")
        if user_profile:
            for folder_name in ["Documents", "Documentos"]:
                candidate = Path(user_profile) / folder_name
                if candidate.exists() and candidate.is_dir():
                    return candidate
        
        documents = os.getenv("DOCUMENTS") or os.getenv("MYDOCUMENTS")
        if documents:
            candidate = Path(documents)
            if candidate.exists() and candidate.is_dir():
                return candidate
    
    elif system == "Darwin":
        return home / "Documents"
    
    candidates = ["Documents", "Documentos", "documents", "documentos"]
    for name in candidates:
        candidate = home / name
        if candidate.exists() and candidate.is_dir():
            return candidate

    xdg_desktop = os.getenv("XDG_DESKTOP_DIR")
    if xdg_desktop:
        return Path(xdg_desktop)
    
    return None
