-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Versión del servidor:         12.0.2-MariaDB - mariadb.org binary distribution
-- SO del servidor:              Win64
-- HeidiSQL Versión:             12.11.0.7065
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Volcando estructura de base de datos para horarios_proyecto
CREATE DATABASE IF NOT EXISTS `horarios_proyecto` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci */;
USE `horarios_proyecto`;

-- Volcando estructura para tabla horarios_proyecto.alumno
CREATE TABLE IF NOT EXISTS `alumno` (
  `IDUsuario` int(11) NOT NULL,
  `NombreAlumno` varchar(100) NOT NULL,
  `Carrera` varchar(100) NOT NULL,
  PRIMARY KEY (`IDUsuario`),
  CONSTRAINT `fk_alumno_usuario` FOREIGN KEY (`IDUsuario`) REFERENCES `usuario` (`IDUsuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.alumnocurso
CREATE TABLE IF NOT EXISTS `alumnocurso` (
  `IDUsuario` int(11) NOT NULL,
  `Clave` varchar(20) NOT NULL,
  `Secc` int(11) NOT NULL,
  PRIMARY KEY (`IDUsuario`,`Clave`,`Secc`),
  KEY `fk_ac_curso` (`Clave`,`Secc`),
  CONSTRAINT `fk_ac_alumno` FOREIGN KEY (`IDUsuario`) REFERENCES `alumno` (`IDUsuario`),
  CONSTRAINT `fk_ac_curso` FOREIGN KEY (`Clave`, `Secc`) REFERENCES `curso` (`Clave`, `Secc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.curso
CREATE TABLE IF NOT EXISTS `curso` (
  `Clave` varchar(20) NOT NULL,
  `Secc` int(11) NOT NULL,
  `Titulo` varchar(100) NOT NULL,
  `Carrera` varchar(100) NOT NULL,
  `IDUsuario` int(11) NOT NULL,
  PRIMARY KEY (`Clave`,`Secc`),
  KEY `fk_curso_profesor` (`IDUsuario`),
  CONSTRAINT `fk_curso_profesor` FOREIGN KEY (`IDUsuario`) REFERENCES `profesor` (`IDUsuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.horario
CREATE TABLE IF NOT EXISTS `horario` (
  `DiaSem` int(11) NOT NULL,
  `Hora` int(11) NOT NULL,
  `Minuto` int(11) NOT NULL,
  `Duracion` int(11) NOT NULL,
  `Semestre` int(11) NOT NULL,
  `Clave` varchar(20) NOT NULL,
  `Secc` int(11) NOT NULL,
  `Titulo` varchar(50) NOT NULL,
  `IDSalon` varchar(10) NOT NULL,
  PRIMARY KEY (`Clave`,`Secc`,`Titulo`,`DiaSem`,`Hora`,`Minuto`),
  KEY `fk_horario_periodo` (`Titulo`),
  KEY `fk_horario_salon` (`IDSalon`),
  CONSTRAINT `fk_horario_curso` FOREIGN KEY (`Clave`, `Secc`) REFERENCES `curso` (`Clave`, `Secc`),
  CONSTRAINT `fk_horario_periodo` FOREIGN KEY (`Titulo`) REFERENCES `periodo` (`Titulo`),
  CONSTRAINT `fk_horario_salon` FOREIGN KEY (`IDSalon`) REFERENCES `salon` (`IDSalon`),
  CONSTRAINT `ck_diasem` CHECK (`DiaSem` between 1 and 7),
  CONSTRAINT `ck_hora` CHECK (`Hora` between 0 and 23),
  CONSTRAINT `ck_minuto` CHECK (`Minuto` between 0 and 59),
  CONSTRAINT `ck_semestre` CHECK (`Semestre` between 1 and 9)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.periodo
CREATE TABLE IF NOT EXISTS `periodo` (
  `Titulo` varchar(50) NOT NULL,
  `FechaInicio` date NOT NULL,
  `FechaFin` date NOT NULL,
  PRIMARY KEY (`Titulo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.profesor
CREATE TABLE IF NOT EXISTS `profesor` (
  `IDUsuario` int(11) NOT NULL,
  `ProfNom` varchar(100) NOT NULL,
  `Tipo` enum('parcial','completo') NOT NULL,
  PRIMARY KEY (`IDUsuario`),
  CONSTRAINT `fk_profesor_usuario` FOREIGN KEY (`IDUsuario`) REFERENCES `usuario` (`IDUsuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.reservacion
CREATE TABLE IF NOT EXISTS `reservacion` (
  `IDReservacion` int(11) NOT NULL AUTO_INCREMENT,
  `Duracion` int(11) NOT NULL,
  `FechaHora` datetime NOT NULL,
  `IDSalon` varchar(10) NOT NULL,
  `IDUsuario` int(11) NOT NULL,
  PRIMARY KEY (`IDReservacion`),
  KEY `fk_res_salon` (`IDSalon`),
  KEY `fk_res_usuario` (`IDUsuario`),
  CONSTRAINT `fk_res_salon` FOREIGN KEY (`IDSalon`) REFERENCES `salon` (`IDSalon`),
  CONSTRAINT `fk_res_usuario` FOREIGN KEY (`IDUsuario`) REFERENCES `usuario` (`IDUsuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.salon
CREATE TABLE IF NOT EXISTS `salon` (
  `IDSalon` varchar(10) NOT NULL,
  `Capacidad` int(11) NOT NULL,
  `Tipo` enum('C','SC','A') NOT NULL,
  PRIMARY KEY (`IDSalon`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla horarios_proyecto.usuario
CREATE TABLE IF NOT EXISTS `usuario` (
  `IDUsuario` int(11) NOT NULL,
  `Nombre` varchar(100) NOT NULL,
  `Tipo` enum('Profesor','Alumno','Administrativo') NOT NULL,
  PRIMARY KEY (`IDUsuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- La exportación de datos fue deseleccionada.

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system'pp) */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
