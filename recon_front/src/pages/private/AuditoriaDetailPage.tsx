import { useParams } from "react-router-dom";
import AuditoriaDetail from "./AuditoriaDetail";

export default function AuditoriaDetailPage() {
  const { auditoriaId } = useParams<{ auditoriaId: string }>();

  if (!auditoriaId) {
    return <div>Auditoría no encontrada.</div>;
  }

  return <AuditoriaDetail key={auditoriaId} id={auditoriaId} />;
}
