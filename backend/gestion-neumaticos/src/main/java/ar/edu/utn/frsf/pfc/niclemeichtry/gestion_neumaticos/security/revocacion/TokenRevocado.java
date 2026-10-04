package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.revocacion;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Token cerrado con logout. Se conserva sólo hasta su expiración natural. */
@Entity
@Table(name = "tokens_revocados", schema = "seguridad")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TokenRevocado {

	@Id
	private UUID jti;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

}
