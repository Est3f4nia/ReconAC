import { useEffect, useState } from "react";
import { fetchAuditorias, fetchEscaneos, fetchEscaneo, deleteEscaneo, moveEscaneo } from "@/data/escaneos";
import type { EscaneoListado, EscaneoResultResponse, AuditoriaResponse } from "@/data/types";
import "@/pages/private/styles/EscaneosListado.css"

export default function EscaneosListado() {
    const [escaneos, setEscaneos] = useState<EscaneoListado[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [escaneoDetalle, setEscaneoDetalle] =
    useState<EscaneoResultResponse | null>(null);

    const [auditorias, setAuditorias] = useState<AuditoriaResponse[]>([]);
    const [escaneoMover, setEscaneoMover] = useState<EscaneoListado | null>(null);
    const [nuevaAuditoriaId, setNuevaAuditoriaId] = useState("");
    const [moving, setMoving] = useState(false);

    const [loadingDetalle, setLoadingDetalle] = useState(false);

    useEffect(() => {
        async function loadData() {
            try {
                setLoading(true);
                setError(null);

                const [escaneosResponse, auditoriasResponse] =
                    await Promise.all([
                        fetchEscaneos(),
                        fetchAuditorias(),
                    ]);

                setEscaneos(escaneosResponse.content);
                setAuditorias(auditoriasResponse);
            } catch (err) {
                setError(
                    err instanceof Error
                        ? err.message
                        : "No se pudieron cargar los datos.",
                );
            } finally {
                setLoading(false);
            }
        }

        loadData();
    }, []);

    if (loading) {
        return (
            <section className="escaneos-listado">
                <h1>Escaneos</h1>
                <p>Cargando escaneos...</p>
            </section>
        );
    }

    if (error) {
        return (
            <section className="escaneos-listado">
                <h1>Escaneos</h1>
                <p className="escaneos-error">{error}</p>
            </section>
        );
    }


    async function handleMover() {
        if (!escaneoMover || !nuevaAuditoriaId) {
            return;
        }

        try {
            setMoving(true);
            setError(null);

            await moveEscaneo(
                escaneoMover.escaneoId,
                nuevaAuditoriaId,
            );

            const nuevaAuditoria = auditorias.find(
                (auditoria) => auditoria.id === nuevaAuditoriaId,
            );

            if (nuevaAuditoria) {
                setEscaneos((actuales) =>
                    actuales.map((escaneo) =>
                        escaneo.escaneoId === escaneoMover.escaneoId
                            ? {
                                ...escaneo,
                                auditoriaId: nuevaAuditoria.id,
                                auditoriaNombre: nuevaAuditoria.nombre,
                            }
                            : escaneo,
                    ),
                );
            }

            setEscaneoMover(null);
            setNuevaAuditoriaId("");
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "No se pudo mover el escaneo.",
            );
        } finally {
            setMoving(false);
        }
    }


    async function handleVerDetalles(escaneo: EscaneoListado) {
        try {
            setLoadingDetalle(true);

            const detalle = await fetchEscaneo(
                escaneo.auditoriaId,
                escaneo.escaneoId,
            );

            setEscaneoDetalle(detalle);
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "No se pudieron cargar los detalles.",
            );
        } finally {
            setLoadingDetalle(false);
        }
    }

    async function handleEliminar(escaneo: EscaneoListado) {
        const confirmar = window.confirm(
            `¿Eliminar el escaneo de la auditoría "${escaneo.auditoriaNombre}"?`,
        );

        if (!confirmar) {
            return;
        }

        try {
            await deleteEscaneo(
                escaneo.auditoriaId,
                escaneo.escaneoId,
            );

            setEscaneos((actuales) =>
                actuales.filter(
                    (item) => item.escaneoId !== escaneo.escaneoId,
                ),
            );
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "No se pudo eliminar el escaneo.",
            );
        }
    }

    function openMoverModal(escaneo: EscaneoListado) {
        setEscaneoMover(escaneo);
        setNuevaAuditoriaId(escaneo.auditoriaId);
    }

    return (
        <section className="escaneos-listado">
            <div className="escaneos-header">
                <div>
                    <h1>Escaneos</h1>
                    <p>Historial de escaneos realizados.</p>
                </div>
            </div>

            {escaneos.length === 0 ? (
                <p className="escaneos-empty">
                    No hay escaneos registrados.
                </p>
            ) : (
                <div className="escaneos-table-wrapper">
                    <table className="escaneos-table">
                        <thead>
                            <tr>
                                <th>Auditoría</th>
                                <th>Objetivos</th>
                                <th>Estado</th>
                                <th>Progreso</th>
                                <th>Nmap</th>
                                <th>Iniciado</th>
                                <th>Completado</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>

                        <tbody>
                            {escaneos.map((escaneo) => (
                                <tr key={escaneo.escaneoId}>
                                    <td>{escaneo.auditoriaNombre}</td>

                                    <td>
                                        <div className="escaneos-objetivos">
                                            {escaneo.objetivos.map((objetivo) => (
                                                <span key={objetivo}>
                                                    {objetivo}
                                                </span>
                                            ))}
                                        </div>
                                    </td>

                                    <td>
                                        <span
                                            className={`status status-${escaneo.estado}`}
                                        >
                                            {escaneo.estado}
                                        </span>
                                    </td>

                                    <td>{escaneo.progreso}%</td>

                                    <td>
                                        {escaneo.nmapVersion ?? "—"}
                                    </td>

                                    <td>
                                        {formatDate(escaneo.iniciadoA)}
                                    </td>

                                    <td>
                                        {formatDate(escaneo.completadoA)}
                                    </td>

                                    <td>
                                        <div className="escaneos-actions">
                                            <button
                                                className="button"
                                                onClick={() => handleVerDetalles(escaneo)}
                                            >
                                                Ver detalles
                                            </button>

                                            <button
                                                className="button"
                                                onClick={() => openMoverModal(escaneo)}
                                                disabled={escaneo.estado === "EN_PROCESO"}
                                            >
                                                Mover
                                            </button>

                                            <button
                                                className="button"
                                                onClick={() => handleEliminar(escaneo)}
                                            >
                                                Eliminar
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            
            {escaneoDetalle && (
                <div className="escaneo-modal">
                    <div className="escaneo-modal-content">
                        <button
                            className="escaneo-modal-close"
                            onClick={() => setEscaneoDetalle(null)}
                        >
                            ×
                        </button>

                        <h2>Detalles del escaneo</h2>

                        {loadingDetalle ? (
                            <p>Cargando...</p>
                        ) : (
                            <pre className="console">
                                {JSON.stringify(
                                    escaneoDetalle.resultado,
                                    null,
                                    2,
                                )}
                            </pre>
                        )}
                    </div>
                </div>
            )}

            {escaneoMover && (
                <div className="escaneo-modal">
                    <div className="escaneo-modal-content">
                        <h2>Mover escaneo</h2>

                        <p>
                            Auditoría actual:
                            <strong>{escaneoMover.auditoriaNombre}</strong>
                        </p>

                        <label htmlFor="nueva-auditoria">
                            Nueva auditoría
                        </label>

                        <select
                            id="nueva-auditoria"
                            value={nuevaAuditoriaId}
                            onChange={(event) =>
                                setNuevaAuditoriaId(event.target.value)
                            }
                        >
                            {auditorias.map((auditoria) => (
                                <option
                                    key={auditoria.id}
                                    value={auditoria.id}
                                >
                                    {auditoria.nombre}
                                </option>
                            ))}
                        </select>

                        <div className="escaneo-modal-actions">
                            <button
                                className="button"
                                onClick={() => {
                                    setEscaneoMover(null);
                                    setNuevaAuditoriaId("");
                                }}
                                disabled={moving}
                            >
                                Cancelar
                            </button>

                            <button
                                className="button"
                                onClick={handleMover}
                                disabled={
                                    moving ||
                                    !nuevaAuditoriaId ||
                                    nuevaAuditoriaId === escaneoMover.auditoriaId
                                }
                            >
                                {moving ? "Moviendo..." : "Mover"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}

function formatDate(value: string | null): string {
    if (!value) {
        return "—";
    }

    return new Date(value).toLocaleString("es-AR");
}