# Radar-Arduino-Uno-SRF08-CM02-RF04

Projet de balayage RADAR comme on peut en voir sur Youtube et tout particulièrement celui-ci: https://howtomechatronics.com/projects/arduino-radar-project/ mais c'est sans l'utilisation du Processing IDE ni l'utilisation du capteur ultra son SR04.

Le matériel utilisé est le suivant:
- une carte Arduino Uno (la doc ne manque pas sur internet)
- un servo moteur standard (Futaba FP-S148 ici)
- un capteur ultrason SRF08 https://www.robot-electronics.co.uk/htm/srf08tech.html
- un émetteur/récepteur USB RF04 https://www.robot-electronics.co.uk/htm/rf04tech.htm
- un émetteur/récepteur I2C CM02 https://www.robot-electronics.co.uk/htm/cm02tech.htm

Il y a donc un projet Java(FX) pour l'affichage du réticule RADAR: ConsoleRadar à ouvrir dans Eclipse et un projet pour l'IDE Arduino: Radar_Servo_SRF08_CM02_RF04.
- La LED est sur le port habituel: 13
- le signal PWM pour le servo est sur le port 9
- Le bus I2C est sur les port A4 et A5, l'adresse de l'Arduino est 8 (car il agit aussi en esclave et le CM02 en maître)
