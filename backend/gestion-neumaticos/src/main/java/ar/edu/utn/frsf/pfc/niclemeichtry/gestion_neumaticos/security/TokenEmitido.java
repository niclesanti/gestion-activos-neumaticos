package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import java.time.Instant;

public record TokenEmitido(String token, Instant expiraEn) {

	@Override
	public String toString() {
		return "TokenEmitido[expiraEn=" + expiraEn + "]";
	}

}
