Sledeča navodila predvidevajo da imate navoljo java in javac,
ter da izvajate ukaze znotraj direktorija teh navodil

Alternativno imam repozitory z nalogo na Githubu:
https://github.com/andrejfox/rk-cahtbot

Najprej vse datoteke prevedemo:
`javac -cp .:json-simple-1.1.1.jar *.java`

Nato lahko zaženemo Strežnik:
`java -cp .:json-simple-1.1.1.jar ChatServer`

ter Odjemalec/ce:
`java -cp .:json-simple-1.1.1.jar ChatClient`

Ob zagonu katerih morate vpisati ime:
```sh
Enter username: Lorem
[system] connecting to chat server ...
[system] connected
[RKchat] <SenderName> [Lorem]: 
>
```

Ime si lahko naknadno spremenimo z `name|n <ime>`:
```sh
>n Ipsom
[RKchat] <SenderName> [Ipsom]: 
>
```

Javna sporočila pošlemo z `public|pu <body>`:
(ukaz ki ga dobijo prejemniki pove tip, kdo ga je poslal ter body sporočila)
```sh
>pu Hallo!
[RKchat] <Public> [Ipsom]: Hallo!
>
```

Privatna sporočila pa se pošilja z `private|pr <recipian> <body>`:
```sh
>pr Ipsom hey
[RKchat] <Private> [Ipsom]: hey
>
```
Če poskusimo poslati pr sporočilo nekomu, ki ni povezan na strežnik dobimo sledeče:
```sh
>pr Joža Hey!
[RKchat] <Error> [Server]: No user with name: Joža
>
```
