
#include <iostream>
#include <unordered_map>
#include <vector>
#include <math.h>
using namespace std;
// class Solution {
//     unordered_map<int, int> parent;
// public:
//     int calSorce(){}

//     int countHighestScoreNodes(vector<int>& parents) {
        
//     }
// };
int main() {
    int target = 1;
    double arr[10] = {210.1, 1159.55, 3081.55, 5731.4, 9164.45,
    13512.6, 18785.9, 24368.5, 30851.7, 37189.3};
    double base = pow(target * 10000, 2);
    for (int i = 1; i <= 10; i++) {
        double val = pow(i * 10000, 2);
        cout << arr[target - 1] * val / base << endl;
    }
}
