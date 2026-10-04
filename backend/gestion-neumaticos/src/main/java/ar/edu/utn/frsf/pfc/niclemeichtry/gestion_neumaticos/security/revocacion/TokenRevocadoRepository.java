package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, UUID> {

	@Modifying
	@Query("delete from TokenRevocado t where t.expiraEn < :instante")
	int eliminarExpiradosAntesDe(Instant instante);

}
