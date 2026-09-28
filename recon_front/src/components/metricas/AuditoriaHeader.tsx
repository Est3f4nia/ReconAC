import {
  MoreVertical,
  Trash2,
  X,
  Pencil,
} from "lucide-react";
import {
  useEffect,
  useRef,
  useState,
} from "react";

import type { AuditoriaResponse } from "@/data/types";
import {
  eliminarAuditoria,
  updateAuditoria,
} from "@/data/auditorias";
import "./styles/AuditoriaHeader.css";

interface Props {
  auditoria: AuditoriaResponse;
  onDeleted: () => void;
  onUpdated?: (
    auditoria: AuditoriaResponse,
  ) => void;
}

export function AuditoriaHeader({
  auditoria,
  onDeleted,
  onUpdated,
}: Props) {
  // menú
  const [menuOpen, setMenuOpen] =
    useState(false);
  const [confirmOpen, setConfirmOpen] =
    useState(false);
  const menuRef =
    useRef<HTMLDivElement | null>(null);

  // actualizar
  const [editando, setEditando] =
    useState(false);
  const [nombre, setNombre] = useState(
    auditoria.nombre ?? "",
  );
  const [objetivo, setObjetivo] = useState(
    auditoria.objetivo ?? "",
  );

  const [saving, setSaving] =
    useState(false);
  const [editError, setEditError] =
    useState("");

  // eliminar
  const [deleting, setDeleting] =
    useState(false);

  const [error, setError] = useState("");

  /*
   * Si la auditoría cambia desde el padre,
   * sincronizamos el formulario de edición.
   */
  useEffect(() => {
    setNombre(auditoria.nombre ?? "");
    setObjetivo(auditoria.objetivo ?? "");
  }, [auditoria]);

  /*
   * Cierra el menú al hacer click afuera.
   */
  useEffect(() => {
    function handleClickOutside(
      event: MouseEvent,
    ) {
      if (
        menuRef.current &&
        !menuRef.current.contains(
          event.target as Node,
        )
      ) {
        setMenuOpen(false);
      }
    }

    document.addEventListener(
      "mousedown",
      handleClickOutside,
    );

    return () => {
      document.removeEventListener(
        "mousedown",
        handleClickOutside,
      );
    };
  }, []);

  function handleOpenEdit() {
    setMenuOpen(false);

    /*
     * Restauramos los valores actuales
     * cada vez que se abre el modal.
     */
    setNombre(auditoria.nombre ?? "");
    setObjetivo(auditoria.objetivo ?? "");

    setEditError("");
    setEditando(true);
  }

  function handleCloseEdit() {
    if (saving) return;

    setEditando(false);
    setEditError("");
  }

  async function handleUpdate() {
    try {
      setSaving(true);
      setEditError("");

      const actualizada =
        await updateAuditoria(
          auditoria.id,
          nombre.trim(),
          objetivo.trim(),
        );

      /*
       * El padre puede reemplazar inmediatamente
       * la auditoría mostrada por la respuesta
       * actualizada del backend.
       */
      onUpdated?.(actualizada);

      setEditando(false);
    } catch (err) {
      setEditError(
        err instanceof Error
          ? err.message
          : "No se pudo actualizar la auditoría.",
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    try {
      setDeleting(true);
      setError("");

      await eliminarAuditoria(
        auditoria.id,
      );

      setConfirmOpen(false);

      onDeleted();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "No se pudo eliminar la auditoría.",
      );
    } finally {
      setDeleting(false);
    }
  }

  return (
    <>
      <header className="dashboard-section auditoria-header">
        <div className="auditoria-header-main">
          <div className="dashboard-section-heading">
            <p className="dashboard-section-eyebrow">
              Auditoría
            </p>

            <h1>{auditoria.nombre}</h1>

            <p>{auditoria.objetivo}</p>
          </div>

          <div
            className="auditoria-header-actions"
            ref={menuRef}
          >
            <button
              type="button"
              className="auditoria-actions-trigger"
              aria-label="Abrir acciones de auditoría"
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              onClick={() =>
                setMenuOpen(
                  (current) => !current,
                )
              }
            >
              <MoreVertical size={18} />
            </button>

            {menuOpen && (
              <div
                className="auditoria-actions-menu"
                role="menu"
              >
                <button
                  type="button"
                  className="auditoria-actions-item"
                  role="menuitem"
                  onClick={handleOpenEdit}
                >
                  <Pencil size={15} />

                  <span>
                    Editar auditoría
                  </span>
                </button>

                <button
                  type="button"
                  className="auditoria-actions-item auditoria-actions-item-danger"
                  role="menuitem"
                  onClick={() => {
                    setMenuOpen(false);
                    setError("");
                    setConfirmOpen(true);
                  }}
                >
                  <Trash2 size={15} />

                  <span>
                    Eliminar auditoría
                  </span>
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="auditoria-header-meta">
          <span>
            Creada:{" "}
            {new Date(
              auditoria.fechaGeneracion,
            ).toLocaleString()}
          </span>

          {auditoria.fechaFinal && (
            <span>
              Finalizada:{" "}
              {new Date(
                auditoria.fechaFinal,
              ).toLocaleString()}
            </span>
          )}
        </div>
      </header>

      {/* =========================
          MODAL EDITAR AUDITORÍA
      ========================== */}

      {editando && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() => {
            if (!saving) {
              handleCloseEdit();
            }
          }}
        >
          <div
            className="modal auditoria-delete-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-auditoria-title"
            onMouseDown={(event) =>
              event.stopPropagation()
            }
          >
            <div className="modal-header">
              <div>
                <p className="dashboard-section-eyebrow">
                  Configuración
                </p>

                <h2 id="edit-auditoria-title">
                  Editar auditoría
                </h2>
              </div>

              <button
                type="button"
                className="modal-close"
                aria-label="Cerrar"
                disabled={saving}
                onClick={handleCloseEdit}
              >
                <X size={18} />
              </button>
            </div>

            <div className="modal-body">
              <div className="field">
                <label htmlFor="auditoria-edit-nombre">
                  Nombre
                </label>

                <input
                  id="auditoria-edit-nombre"
                  type="text"
                  value={nombre}
                  maxLength={50}
                  disabled={saving}
                  placeholder="Nueva auditoría"
                  onChange={(event) =>
                    setNombre(
                      event.target.value,
                    )
                  }
                />

                <span className="field-counter">
                  {nombre.length}/50
                </span>
              </div>

              <div className="field">
                <label htmlFor="auditoria-edit-objetivo">
                  Objetivo
                </label>

                <textarea
                  id="auditoria-edit-objetivo"
                  value={objetivo}
                  maxLength={150}
                  rows={4}
                  disabled={saving}
                  placeholder="Objetivo de la auditoría"
                  onChange={(event) =>
                    setObjetivo(
                      event.target.value,
                    )
                  }
                />

                <span className="field-counter">
                  {objetivo.length}/150
                </span>
              </div>

              <p className="auditoria-edit-help">
                Si no se especifica un nombre,
                se utilizará "Nueva auditoría".
              </p>

              {editError && (
                <div
                  className="dashboard-error"
                  role="alert"
                >
                  {editError}
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="button button-audit-header"
                disabled={saving}
                onClick={handleCloseEdit}
              >
                Cancelar
              </button>

              <button
                type="button"
                className="button button-audit-header"
                disabled={saving}
                onClick={() => {
                  void handleUpdate();
                }}
              >
                {saving
                  ? "Guardando..."
                  : "Guardar cambios"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* =========================
          MODAL ELIMINAR AUDITORÍA
      ========================== */}

      {confirmOpen && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() => {
            if (!deleting) {
              setConfirmOpen(false);
            }
          }}
        >
          <div
            className="modal auditoria-delete-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-auditoria-title"
            onMouseDown={(event) =>
              event.stopPropagation()
            }
          >
            <div className="modal-header">
              <div>
                <p className="dashboard-section-eyebrow">
                  Confirmación
                </p>

                <h2 id="delete-auditoria-title">
                  Eliminar auditoría
                </h2>
              </div>

              <button
                type="button"
                className="modal-close"
                aria-label="Cerrar"
                disabled={deleting}
                onClick={() =>
                  setConfirmOpen(false)
                }
              >
                <X size={18} />
              </button>
            </div>

            <div className="modal-body">
              <p>
                Vas a eliminar: auditoría{" "}
                <strong>
                  {auditoria.nombre}
                </strong>
                .
              </p>

              <p>
                Esta acción es irreversible.
                Se eliminarán también los
                escaneos, activos, puertos y
                asociaciones dependientes de
                esta auditoría.
              </p>

              <div className="modal-delete-warning">
                <p>
                  Esta acción es irreversible.
                </p>
              </div>

              {error && (
                <div
                  className="dashboard-error"
                  role="alert"
                >
                  {error}
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="button button-audit-header"
                disabled={deleting}
                onClick={() =>
                  setConfirmOpen(false)
                }
              >
                Cancelar
              </button>

              <button
                type="button"
                className="button button-danger"
                disabled={deleting}
                onClick={() => {
                  void handleDelete();
                }}
              >
                {deleting
                  ? "Eliminando..."
                  : "Eliminar auditoría"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}