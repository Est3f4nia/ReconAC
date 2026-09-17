import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import {
  KeyRound,
  X,
} from "lucide-react";

import { updateNvdApiKey } from "@/data/usuarios";
import "./UserModal.css";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function UserModal({
  open,
  onClose,
}: Props) {
  const [editingKey, setEditingKey] =
    useState(false);

  const [apiKey, setApiKey] =
    useState("");

  const [confirmApiKey, setConfirmApiKey] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const [success, setSuccess] =
    useState("");

  useEffect(() => {
    if (!open) {
      setEditingKey(false);
      setApiKey("");
      setConfirmApiKey("");
      setError("");
      setSuccess("");
      setLoading(false);
    }
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }

    function handleKeyDown(
      event: KeyboardEvent,
    ) {
      if (
        event.key === "Escape" &&
        !loading
      ) {
        onClose();
      }
    }

    document.addEventListener(
      "keydown",
      handleKeyDown,
    );

    return () => {
      document.removeEventListener(
        "keydown",
        handleKeyDown,
      );
    };
  }, [
    open,
    loading,
    onClose,
  ]);

  if (!open) {
    return null;
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    const normalized =
      apiKey.trim();

    setError("");
    setSuccess("");

    if (!normalized) {
      setError(
        "Ingresá una API KEY.",
      );

      return;
    }

    if (
      normalized !==
      confirmApiKey.trim()
    ) {
      setError(
        "Las API KEY no coinciden.",
      );

      return;
    }

    try {
      setLoading(true);

      await updateNvdApiKey(
        normalized,
      );

      setApiKey("");
      setConfirmApiKey("");

      setSuccess(
        "La API KEY fue actualizada correctamente.",
      );

      setEditingKey(false);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo actualizar la API KEY.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (
          event.target ===
            event.currentTarget &&
          !loading
        ) {
          onClose();
        }
      }}
    >
      <div
        className="modal auditoria-delete-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="user-modal-title"
      >
        <div className="modal-header">
          <div>
            <p className="dashboard-section-eyebrow">
              Cuenta
            </p>

            <h2 id="user-modal-title">
              Configuración de usuario
            </h2>
          </div>

          <button
            type="button"
            className="modal-close"
            aria-label="Cerrar"
            onClick={onClose}
            disabled={loading}
          >
            <X size={20} />
          </button>
        </div>

        {!editingKey ? (
          <div className="user-modal-options">
            <button
              type="button"
              className="user-modal-option"
              onClick={() => {
                setEditingKey(true);
                setError("");
                setSuccess("");
              }}
            >
            
              <KeyRound
                size={20}
                aria-hidden="true"
              />
            <div className="user-modal-option-content">
              <span>
                <strong>Actualizar API KEY</strong>

                <small>
                  Reemplazar la clave de acceso utilizada para NVD.
                </small>
              </span>
            </div>
              
            </button>

            {success && (
              <div className="dashboard-success">
                {success}
              </div>
            )}
          </div>
        ) : (
          <form
            className="user-api-key-form"
            onSubmit={handleSubmit}
          >
            <div className="form-group">
              <label htmlFor="new-nvd-api-key">
                Nueva NVD API KEY
              </label>

              <input
                id="new-nvd-api-key"
                type="password"
                value={apiKey}
                onChange={(event) =>
                  setApiKey(
                    event.target.value,
                  )
                }
                autoComplete="new-password"
                disabled={loading}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirm-nvd-api-key">
                Confirmar API KEY
              </label>

              <input
                id="confirm-nvd-api-key"
                type="password"
                value={confirmApiKey}
                onChange={(event) =>
                  setConfirmApiKey(
                    event.target.value,
                  )
                }
                autoComplete="new-password"
                disabled={loading}
                required
              />
            </div>

            <p className="user-api-key-help">
              La clave actual no se muestra por
              seguridad. Al confirmar, será
              reemplazada por la nueva.
            </p>

            {error && (
              <div className="dashboard-error">
                {error}
              </div>
            )}

            <div className="modal-actions">
              <button
                type="button"
                className="button button-secondary"
                disabled={loading}
                onClick={() => {
                  setEditingKey(false);
                  setApiKey("");
                  setConfirmApiKey("");
                  setError("");
                }}
              >
                Volver
              </button>

              <button
                type="submit"
                className="button"
                disabled={
                  loading ||
                  !apiKey.trim() ||
                  !confirmApiKey.trim()
                }
              >
                {loading
                  ? "Actualizando..."
                  : "Actualizar API KEY"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}