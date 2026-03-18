Bio Keeper: Sistema Inteligente de Preservação de Nutrientes via IoT
O Bio Keeper é uma solução avançada de IoT (Internet das Coisas) projetada para otimizar a cadeia de consumo de alimentos orgânicos. Desenvolvido em linguagem C para microcontroladores, o sistema integra climatização de precisão com a tecnologia de estimulação lumínica por espectro ativo, visando mitigar o desperdício alimentar e preservar a densidade nutricional de hortaliças.

Este projeto foi desenvolvido pelo 1º Semestre de Análise e Desenvolvimento de Sistemas da FATEC Indaiatuba (2026).

Proposta de Valor e Diferencial Comercial
Diferente de sistemas de refrigeração convencionais, que atuam apenas na redução da atividade bacteriana através do frio, o Bio Keeper foca na manutenção da vitalidade celular do vegetal.

Tecnologia de Fotossíntese Simulada
Baseado em estudos de bioengenharia aplicados por grandes players do setor de eletrodomésticos, o Bio Keeper utiliza LEDs de comprimentos de onda específicos (Azul e Verde) para simular o ciclo circadiano natural. Este processo mantém os estômatos das plantas em funcionamento controlado, preservando níveis de Vitamina C e clorofila por períodos significativamente superiores aos métodos de armazenamento comuns.

Monitoramento de Integridade e Log de Dados
O sistema conta com um módulo de inteligência de dados que registra oscilações térmicas e períodos de inatividade energética. Comercialmentes, isso se traduz em um Certificado de Integridade do Alimento, permitindo que o consumidor ou o estabelecimento comercial tenha prova documental de que o produto não sofreu estresse térmico acima dos limites toleráveis.

Arquitetura Técnica

(Componentes do Arduino utilizados no projeto)


Atuadores Bióticos: Matriz de LEDs RGB/UV com controle de PWM para transição suave de luminosidade.



Funcionalidades Principais
Climatização Adaptativa: Ajuste de resfriamento baseado em setpoints específicos para diferentes tipos de orgânicos.

Ciclo de Luz Programável: Automação de fotoperíodo para simulação de ambiente natural.

Relatório de Log: Histórico detalhado de tempo de operação e detecção de quedas de energia.

Eficiência Energética: Algoritmos de controle que reduzem o consumo de energia em períodos de estabilidade térmica.



Instalação e Configuração
Para replicar o ambiente de desenvolvimento em escala laboratorial:

Realize o clone deste repositório: `git clone https://github.com/gmribeiro/Projeto-Arduino-BioKeeper.git`



Equipe de Desenvolvimento (ADS - FATEC Indaiatuba)
Arthur Lima Batista

Gabriel de Melo Ribeiro

Guilherme Bianchi Machado

Guilherme Pepe Ferreira

Gustavo Gomes Andrade

Gustavo Henrique Gonçalves Barbuena
Instale as bibliotecas de suporte (DHT, LiquidCrystal_I2C e RTClib) na sua IDE de preferência.

Compile e faça o upload do firmware para o hardware alvo.
