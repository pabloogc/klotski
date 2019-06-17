# klotski
Klotski solver in kotlin script

To run: 

`kotlinc -script klotski.kts`

Outputs list of moves to solve the problem in minimal amount of steps:

```
f(3,3) -> BOTTOMx1
XXXXXX
XabbcX
XabbcX
Xee__X
XdgfjX
XdifhX
XX__XX
---------------------
e(1,3) -> RIGHTx1
XXXXXX
XabbcX
XabbcX
X_ee_X
XdgfjX
XdifhX
XX__XX
---------------------
e(1,3) -> RIGHTx2
XXXXXX
XabbcX
XabbcX
X__eeX
XdgfjX
XdifhX
XX__XX

...
```
