using Output;

var(INT) NUMBER = 132;
var(BOOLEAN) boolean = false;

NUMBER = Output.input("Digite um numero: ")(INT);

NUMBER = NUMBER*2;

Output.println("O dobro do Numero digitado foi: {NUMBER}");