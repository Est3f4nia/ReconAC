import { useEffect, useState } from "react";
import {
    fetchActivos,
} from "@/data/activos";
import type {
    ActivoAgrupadoResponse,
} from "@/data/types";
import "@/pages/private/styles/ActivosListado.css";

const PAGE_SIZE = 20;

export default function ActivosListado() {
    const [activos, setActivos] = useState<ActivoAgrupadoResponse[]>([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [activoDetalle, setActivoDetalle] =
        useState<ActivoAgrupadoResponse | null>(null);

    const cargarActivos = async () => {
        try {
            setLoading(true);
            setError(null);

            const response = await fetchActivos(page, PAGE_SIZE);

            setActivos(response.content);
            setTotalPages(response.totalPages);
            setTotalElements(response.totalElements);
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "No se pudieron cargar los activos."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void cargarActivos();
    }, [page]);

    const siguientePagina = () => {
        if (page < totalPages - 1) {
            setPage((prev) => prev + 1);
        }
    };

    const paginaAnterior = () => {
        if (page > 0) {
            setPage((prev) => prev - 1);
        }
    };

    if (loading) {
        return (
            <section className="activos-page">
                <div className="activos-header">
                    <h1>Activos</h1>
                    <p>Cargando activos...</p>
                </div>

                <div className="activos-loading">
                    Cargando...
                </div>
            </section>
        );
    }

    if (error) {
        return (
            <section className="activos-page">
                <div className="activos-header">
                    <h1>Activos</h1>
                </div>

                <div className="activos-error">
                    <p>{error}</p>

                    <button
                        type="button"
                        className="button button-error"
                        onClick={() => void cargarActivos()}
                    >
                        Reintentar
                    </button>
                </div>
            </section>
        );
    }

    return (
        <section className="activos-page">
            <div className="activos-header">
                <div>
                    <h1>Activos</h1>
                    <p>
                        Activos detectados en tus escaneos.
                    </p>
                </div>

                <span className="activos-total">
                    {totalElements}{" "}
                    {totalElements === 1 ? "activo" : "activos"}
                </span>
            </div>

            {activos.length === 0 ? (
                <div className="activos-empty">
                    <p>No hay activos registrados.</p>
                </div>
            ) : (
                <>
                    <div className="activos-table-container">
                        <table className="activos-table">
                            <thead>
                                <tr>
                                    <th>Host</th>
                                    <th>Hostname</th>
                                    <th>Sistema operativo</th>
                                    <th>Probabilidad</th>
                                    <th>MAC</th>
                                    <th>Descripción</th>
                                    <th>Escaneos</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>

                            <tbody>
                                {activos.map((activo) => (
                                    <tr
                                        key={[
                                            activo.host,
                                            activo.hostname,
                                            activo.so,
                                        ].join("|")}
                                    >
                                        <td>
                                            <code>{activo.host}</code>
                                        </td>

                                        <td>
                                            {activo.hostname ?? "—"}
                                        </td>

                                        <td>
                                            {activo.so ?? "—"}
                                        </td>

                                        <td>
                                            {activo.soProbab != null
                                                ? `${activo.soProbab}%`
                                                : "—"}
                                        </td>

                                        <td>
                                            {activo.mac ?? "—"}
                                        </td>

                                        <td>
                                            {activo.descripcion ?? "—"}
                                        </td>

                                        <td>
                                            {activo.escaneoIds.length}
                                        </td>

                                        <td>
                                            <button
                                                type="button"
                                                className="button"
                                                onClick={() =>
                                                    setActivoDetalle(activo)
                                                }
                                            >
                                                Ver
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="activos-pagination">
                        <button
                            type="button"
                            className="button"
                            onClick={paginaAnterior}
                            disabled={page === 0}
                        >
                            Anterior
                        </button>

                        <span>
                            Página {page + 1} de{" "}
                            {Math.max(totalPages, 1)}
                        </span>

                        <button
                            type="button"
                            className="button"
                            onClick={siguientePagina}
                            disabled={
                                page >= totalPages - 1
                            }
                        >
                            Siguiente
                        </button>
                    </div>
                </>
            )}

            {activoDetalle && (
                <div
                    className="activo-modal-overlay"
                    onClick={() => setActivoDetalle(null)}
                >
                    <div
                        className="activo-modal"
                        onClick={(event) =>
                            event.stopPropagation()
                        }
                    >
                        <div className="activo-modal-header">
                            <h2>Detalle del activo</h2>

                            <button
                                type="button"
                                className="activo-modal-close"
                                onClick={() =>
                                    setActivoDetalle(null)
                                }
                                aria-label="Cerrar"
                            >
                                ×
                            </button>
                        </div>

                        <div className="activo-detalle">
                            <div className="activo-detalle-item">
                                <strong>Host</strong>
                                <code>
                                    {activoDetalle.host}
                                </code>
                            </div>

                            <div className="activo-detalle-item">
                                <strong>Hostname</strong>
                                <span>
                                    {activoDetalle.hostname ?? "—"}
                                </span>
                            </div>

                            <div className="activo-detalle-item">
                                <strong>
                                    Sistema operativo
                                </strong>
                                <span>
                                    {activoDetalle.so ?? "—"}
                                </span>
                            </div>

                            <div className="activo-detalle-item">
                                <strong>
                                    Probabilidad del SO
                                </strong>
                                <span>
                                    {activoDetalle.soProbab != null
                                        ? `${activoDetalle.soProbab}%`
                                        : "—"}
                                </span>
                            </div>

                            <div className="activo-detalle-item">
                                <strong>MAC</strong>
                                <span>
                                    {activoDetalle.mac ?? "—"}
                                </span>
                            </div>

                            <div className="activo-detalle-item">
                                <strong>Descripción</strong>
                                <span>
                                    {activoDetalle.descripcion ?? "—"}
                                </span>
                            </div>

                            <div className="activo-detalle-escaneos">
                                <strong>
                                    Escaneos donde fue detectado
                                </strong>

                                {activoDetalle.escaneoIds.length ===
                                0 ? (
                                    <span>
                                        Sin escaneos registrados.
                                    </span>
                                ) : (
                                    <div className="activo-escaneos-lista">
                                        {activoDetalle.escaneoIds.map(
                                            (escaneoId) => (
                                                <code
                                                    key={escaneoId}
                                                >
                                                    {escaneoId}
                                                </code>
                                            )
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="activo-modal-footer">
                            <button
                                type="button"
                                className="button"
                                onClick={() =>
                                    setActivoDetalle(null)
                                }
                            >
                                Cerrar
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}